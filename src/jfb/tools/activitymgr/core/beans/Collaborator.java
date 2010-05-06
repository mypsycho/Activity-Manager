/*
 * Copyright (c) 2004, Jean-Fran�ois Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jfb.tools.activitymgr.core.beans;

/**
 * Collaborateur.
 */
public class Collaborator extends SimpleIdentityBean {

	/** Identifiant du collaborateur */
	private String login;

	/** Nom */
	private String firstName;
	
	/** Pr�nom */
	private String lastName;
	
	/**
	 * @return le pr�nom du collaborateur.
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * D�finit le pr�nom du collaborateur.
	 * @return le nouveau pr�nom.
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return le nom du collaborateur.
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * D�finit le nom du collaborateur.
	 * @return le nouveau nom.
	 */
	public void setLastName(String lasstName) {
		this.lastName = lasstName;
	}

	/**
	 * @return l'identifiant du collaborateur.
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * D�finit l'identifiant du collaborateur.
	 * @return le nouvel identifiant.
	 */
	public void setLogin(String login) {
		this.login = login;
	}
}
