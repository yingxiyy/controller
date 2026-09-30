/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.webapp;

import javax.servlet.http.HttpServletRequest;

/**
 * @version 1.0
 */
public class RequestAttributes {
  public final static String RequestID = "requestSeqID";


  private final static String API_PATTERN = "^\\/restconf\\/(.+)$";

  public static boolean isAPI(HttpServletRequest request) {
    String requestURI = request.getRequestURI();
    if(requestURI.matches(API_PATTERN))  // Check if the requested URL is not a controller (/restconf/**)
      return true;
    else
      return false;
  }
}
