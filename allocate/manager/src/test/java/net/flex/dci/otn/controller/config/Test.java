/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.config;
/**
 * @author YYX
 * @version 1.0
 */
public class Test {
  public static void main(String[] args) {
    String t = "/odu4=1";
    String[] tmp = t.split("/");
    System.out.println(tmp.length);
    System.out.println(tmp[0] + "," + tmp[0].length());

    String slot = "4";
    tmp = slot.split(",");
    System.out.println(tmp.length);
    System.out.println(tmp[0] + "," + tmp[0].length());
    String avaOdujString = "4";
//    List<Integer> idsList = Arrays.asList(avaOdujString.split(","))
//            .stream().map(s -> Integer.parseInt(s.trim())).collect(Collectors.toList());
    int pos = avaOdujString.indexOf(slot);
    String newStr = null;
    if (pos == 0) {
      //we need remove this slot from available list
      if (avaOdujString.length() == slot.length()) {
        newStr = "";
      } else {
        newStr = avaOdujString.substring(slot.length() + 1); // + 1 for comma
      }
    } else if (pos > 0) {
      if ((pos + slot.length() + 1) > avaOdujString.length()) {
        //this is latest one value in string
        newStr = avaOdujString.substring(0, pos - 1); //previous comma
      } else {
        //middle
        newStr = avaOdujString.substring(0, pos);
        newStr += avaOdujString.substring(pos + slot.length() + 1, avaOdujString.length());
      }
    }
    System.out.println(newStr);
  }

}
