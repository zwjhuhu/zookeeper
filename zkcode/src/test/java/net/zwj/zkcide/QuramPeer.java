package net.zwj.zkcide;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class QuramPeer {

	private int zxid;
	private int myid;
	private int port;
	private int electionEpoch;
	private int[] peerPorts;
	private Vote vote;
	private int size;
	private Socket[] scs;

	private String name;

	private LinkedBlockingQueue<Vote> queue = new LinkedBlockingQueue<>();

	private List<Vote> votes;

	public QuramPeer(int zxid, int myid, int port, int electionEpoch, int[] ports) {
		this.zxid = zxid;
		this.myid = myid;
		this.port = port;
		this.electionEpoch = electionEpoch;
		this.size = ports.length - 1;
		peerPorts = new int[size];
		for (int i = 0, k = 0; i < ports.length; i++) {
			if (ports[i] != port) {
				peerPorts[k++] = ports[i];
			}
		}

		scs = new Socket[this.size];
		this.vote = new Vote();
		this.vote.setElectionEpoch(electionEpoch);
		this.vote.setMyid(myid);
		this.vote.setZxid(zxid);
		this.votes = new ArrayList<>(this.size);
		this.name = "perr-" + port;
	}

	@SuppressWarnings("resource")
	public void start() {
		System.out
				.println("begin election " + name + " from zxid " + zxid + " myid " + myid + " epoch " + electionEpoch);

		ElectionThread listenThread = new ElectionThread(port);
		new Thread(listenThread).start();
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		for (int i = 0; i < size; i++) {
			Socket s = new Socket();
			try {
				s.connect(new InetSocketAddress("127.0.0.1", peerPorts[i]));
				scs[i] = s;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sendVote();
		while (true) {
			try {
				Vote v = queue.poll(10, TimeUnit.SECONDS);
				if (!checkAndModifyVote(v)) {
					sendVote();
				} else {
					System.out.println("election over " + name + " find leader " + v.getMyid());
					for (int i = 0; i < size; i++) {
						scs[i].close();
					}
					listenThread.shutdown();
					break;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private boolean checkAndModifyVote(Vote v) {
		if (v == null) {
			return false;
		}

		int epoch = v.getElectionEpoch();
		if (epoch > electionEpoch) {
			vote.setElectionEpoch(epoch);
			electionEpoch = epoch;
			return false;
		} else if (epoch < electionEpoch) {
			return false;
		}
		if (v.getZxid() > vote.getZxid()) {
			vote.setZxid(v.getZxid());
			vote.setMyid(v.getMyid());
			return false;
		} else if (v.getZxid() == vote.getZxid() && v.getMyid() > vote.getMyid()) {
			vote.setMyid(v.getMyid());
			return false;
		} else if (v.getZxid() < vote.getZxid() || v.getMyid() < vote.getMyid()) {
			return false;
		}
		if (!votes.contains(v)) {
			boolean needClear = false;
			for (Vote tmp : votes) {
				if (tmp.getZxid() != v.getZxid() || tmp.getMyid() != v.getMyid()) {
					needClear = true;
				}
			}
			if (needClear) {
				votes.clear();
			}
			votes.add(v);
		}
		if (votes.size() > size / 2) {
			return true;
		}
		return false;

	}

	private void sendVote() {
		for (int i = 0; i < size; i++) {
			Socket s = scs[i];
			try {
				if (s != null && s.isConnected()) {
					OutputStream out = s.getOutputStream();
					//System.out.println(vote);
					out.write(vote.toString().getBytes());
				}
			} catch (Exception e) {
				System.out.println(vote);
				e.printStackTrace();
			}
		}
	}

	class ElectionThread implements Runnable {
		private int port;
		private ServerSocket ss;
		private volatile boolean runFlag = true;

		ElectionThread(int port) {
			this.port = port;
		}

		public void shutdown() {
			runFlag = false;
			if (ss != null) {
				try {
					ss.close();
				} catch (Exception e) {
					// TODO: handle exception
				}
			}

		}

		@Override
		public void run() {
			try {
				ss = new ServerSocket(port);
				int index = 0;
				while (runFlag) {
					Socket sc = ss.accept();
					new Thread(new RecvThread(index++, sc)).start();
				}
			} catch (IOException e) {
				// e.printStackTrace();
			}

		}

	}

	class RecvThread implements Runnable {

		private int index;
		private Socket sc;

		RecvThread(int index, Socket sc) {
			this.index = index;
			this.sc = sc;
		}

		void putVote(byte[] cont) throws InterruptedException {
			String str = new String(cont);
			//System.out.println(str);
			String[] s = str.split(",");
			Vote v = new Vote();
			v.setElectionEpoch(Integer.parseInt(s[0]));
			v.setZxid(Integer.parseInt(s[1]));
			v.setMyid(Integer.parseInt(s[2]));
			v.setIndex(index);
			queue.put(v);
		}

		@Override
		public void run() {
			InputStream in = null;
			int contentLen = 11;
			byte[] buf = new byte[100];
			byte[] cont = new byte[contentLen];
			try {
				in = sc.getInputStream();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				int len = -1;
				int offset = 0;
				int left = 0;
				int start = 0;

				while (offset < contentLen) {
					len = in.read(buf);
					if (len == -1) {
						break;
					} else if (len == 0) {
						continue;
					}
					left = len;
					start = 0;
					while (left + offset >= contentLen) {
						for (; offset < contentLen; offset++, start++) {
							cont[offset] = buf[start];
						}
						putVote(cont);
						if(start<contentLen)
							left -= start;
						else {
							left -= contentLen;
						}
						offset = 0;
					}
					for (; left>0; offset++, start++,left--) {
						cont[offset] = buf[start];
					}

				}
			} catch (Exception e) {
				// e.printStackTrace();
			} finally {
				try {
					sc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
