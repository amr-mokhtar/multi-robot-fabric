/*
 * Copyright (c) Lukas Kolisko.
 * All Rights Reserved.
 */

/*
 * Copyright (c) 2018 Amr Mokhtar.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.dcu;

import java.io.Serializable;
import java.util.Set;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

public class RUser implements User, Serializable {

	private static final long serialVersionUID = 1L;
	//private static final long serializationId = 1L;

	private String name;
	private Set<String> roles;
	private String account;
	private String affiliation;
	private Enrollment enrollment;
	private String mspId;

	public RUser() {
		// no-arg constructor
	}

	public RUser(String name, String affiliation, String mspId, Enrollment enrollment) {
		this.name = name;
		this.affiliation = affiliation;
		this.enrollment = enrollment;
		this.mspId = mspId;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	@Override
	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	@Override
	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

	@Override
	public Enrollment getEnrollment() {
		return enrollment;
	}

	public void setEnrollment(Enrollment enrollment) {
		this.enrollment = enrollment;
	}

	@Override
	public String getMspId() {
		return mspId;
	}

	public void setMspId(String mspId) {
		this.mspId = mspId;
	}

	@Override
	public String toString() {
		return "Robot[" + "name='" + name + '\'' +
				"| roles=" + roles +
				"| account='" + account + '\'' +
				"| affiliation='" + affiliation + '\'' +
				"| enrollment=" + enrollment +
				"| mspId='" + mspId + "\']";
	}
}
