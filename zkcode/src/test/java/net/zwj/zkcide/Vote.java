package net.zwj.zkcide;

import java.io.Serializable;

public class Vote implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6666338914734261136L;
	
	private int index;
	private int electionEpoch;
	private int myid;
	private int zxid;
	public int getMyid() {
		return myid;
	}
	public void setMyid(int myid) {
		this.myid = myid;
	}
	public int getElectionEpoch() {
		return electionEpoch;
	}
	public void setElectionEpoch(int electionEpoch) {
		this.electionEpoch = electionEpoch;
	}
	public int getZxid() {
		return zxid;
	}
	public void setZxid(int zxid) {
		this.zxid = zxid;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + electionEpoch;
		result = prime * result + index;
		result = prime * result + myid;
		result = prime * result + zxid;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vote other = (Vote) obj;
		if (electionEpoch != other.electionEpoch)
			return false;
		if (index != other.index)
			return false;
		if (myid != other.myid)
			return false;
		if (zxid != other.zxid)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "00"+electionEpoch+","+zxid+",00"+myid;
	}
	
}
