package com.vistara.gateway.syslog.config;

import com.opsramp.gateway.common.db.annotation.Column;
import com.opsramp.gateway.common.db.annotation.Persistable;
import com.opsramp.gateway.common.db.annotation.PrimaryKey;
import com.opsramp.gateway.common.db.model.SqlPersistablePOJO;

//
//import com.vistara.gateway.db.annotation.Column;
//import com.vistara.gateway.db.annotation.Persistable;
//import com.vistara.gateway.db.annotation.PrimaryKey;
//import com.vistara.gateway.db.model.SqlPersistablePOJO;

@SuppressWarnings("serial")
@Persistable(engine = "SQL")
public class SyslogConfPojo extends SqlPersistablePOJO {

	@PrimaryKey
	@Column(name = "uniqueId", datatype = "varchar(255)")
	private String uniqueId;

	@Column(name = "payload", datatype = "text")
	private String payload;

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

}