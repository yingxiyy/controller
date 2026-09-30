/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class Tian {

    @Test
    public void testFor(){
        ArrayList<Integer> list=new ArrayList<>(Arrays.asList(1,2));
        list.add(1,3);
        for(Integer item:list){
            System.out.println(item);
        }
    }

    @Test
    public void testEmpty(){
        List<String> testLIst= Arrays.asList("1","2","3");
        Map<String,String> result=testLIst.stream().filter(i->i.equals("8")).collect(
                Collectors.toMap(i->i,i -> i));
        Assert.assertFalse(result==null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testSplit() {
        String test="MUX-1-50-M1D1";
        String[] split = test.split("-",2);
        System.out.println(split[1]);
        /*Assert.assertNotEquals(null,split);
        Assert.assertEquals(1,split.length);
        for(String s: split){
            System.out.println(s);
        }*/
    }

    @Test
    public void testMatch() {
        String test = "MUX-1-50-M1D1";
        Assert.assertFalse(test.matches("M\\d+D\\d+"));

        test = "M1D1";
        Assert.assertTrue(test.matches("M\\d+D\\d+"));

        test = "M96D96";
        Assert.assertTrue(test.matches("M\\d+D\\d+"));

        test = "Line";
        Assert.assertFalse(test.matches("M\\d+D\\d+"));
        /*Assert.assertNotEquals(null,split);
        Assert.assertEquals(1,split.length);
        for(String s: split){
            System.out.println(s);
        }*/
    }
}
