/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.kafka;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author YYX
 * @version 1.0
 */
public class Test {

  public static void main (String[] argv) {
    Date date = new Date();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String today = simpleDateFormat.format(date.getTime());
    System.out.println("当天日期" + today);

    //1、当天凌晨(毫秒)
    try {
      long daytime1 = simpleDateFormat.parse(today).getTime();
      System.out.println("1、当天凌晨(毫秒)" + daytime1);
    } catch (Exception e) {
      e.printStackTrace();
    }

    //3、当天凌晨(毫秒)
    Calendar c = Calendar.getInstance();
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    long daytime3 = c.getTimeInMillis();
    System.out.println("3、当天凌晨(毫秒)" + daytime3);

//    c.setTime(date);
    c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    long daytime4 = c.getTimeInMillis();
    System.out.println("3、当天凌晨(毫秒)" + daytime4);
  }
  public static void main1(String[] args) {
    List<String> a = new LinkedList<>();
    String name = a.getClass().getSimpleName();
    System.out.println(name);

    a.add("1");
    a.add("2");
    a.add("3");
    a.add("4");
    System.out.println(a.size());

    List<String> b = new LinkedList<>();
    b.add("b1");
    b.add("b2");
    a.addAll(a.size()-1,b);  //1,2,3,b1,b2,4
//    a.addAll(0,b);  //b1,b2,1,2,3,4
    System.out.println(a);

    Test t = new Test();
    t.tester();
  }

  private void tester() {
    Father t = new Father();
    t.point();    //output: 1

    Father s = new Son();
    s.point();  //here is son 1
  }

  class Father {
    int f = 1;

    public void point() {
      System.out.println(f);
    }
  }

  class Son extends Father {
    public void point() {
      System.out.print("here is son ");
      super.point();
    }
  }
}
