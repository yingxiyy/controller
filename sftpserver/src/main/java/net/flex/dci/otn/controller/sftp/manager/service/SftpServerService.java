/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.service;

import net.flex.dci.otc.common.exception.CommonException;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/8 13:13
 */
public interface SftpServerService {

    String createServer(String request) throws CommonException;

    String updateServer(String request) throws CommonException;

    String deleteServer(String request) throws CommonException;

    String uploadFileToFtpServer(String request) throws CommonException;

    String downloadFileFromFtpServer(String request) throws CommonException;

    String retrieveFileFromFtpServerDirectory(String request) throws CommonException;

    String mkdirForFtpServer(String request) throws CommonException;

    String rmFtpFile(String request) throws CommonException;

    String rmFtpFolder(String request) throws CommonException;
}
