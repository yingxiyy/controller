/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.util;

import java.math.BigDecimal;

public class Constant {    public static final String OCH_AVAILABLE_ODU_SEPARATOR = "-";
    public static final int friendlyNameLength = 64;
    public static final int maxLength = 255;
    public static final String INTERVAL = "_MODEL_";
    public static final String defLocation = "38";
    public static final String Location_Interval = "2";
    public static final String CHASSIS = "CHASSIS";
    public static final String LINECARD = "LINECARD";
    public static final String KEY_RISK_PLANE = "riskPlane";
    public static String EXPROUTE_TAG = "explict-route";
    public static String HASH_TAG = "#";
    public static String INTERLINK_TAG = "internal";
    public static String TUNNEL_TAG = "tunnel";
    public static String EMPTY = "EMPTY";
    public static String EMPTY_IP = "0.0.0.0";
    public static String NOT_DEFINED = "NOT DEFINED";
    public static String OCH_TOPOID = "och-topology";
    public static String VIEWSITE_TOPOID = "site-view-topology";
    public static String PHY_TOPOID = "otn-phy-topology";
    public static String SITE_TOPOID = "site-topology";
    public static String SITE_TAG = "site";
    public static String VIEW_TAG = "view";
    public static String PHY_TAG = "phy";
    public static String LINK_TAG = "link";
    public static String NODE_TAG = "node";
    public static String TP_TAG = "tp";
    public static String XC_TAG = "xc";
    public static String EQUIP_TAG = "equip";
    public static String OCH_PREFIX = "OCH_";
    public static String KEY_TOPO = "network-topology";
    public static String KEY_SCHEDULES = "schedules";
    public static String KEY_FTP = "ftp-servers";
    public static String RACK_NUMBER = "TMP-TMP";
    public static Integer PA_VOA = 3;
    public static Integer ILA_VOA = 3;


    public enum UpdateProgress {
        none, partial, all;
    }

//    public static final class Role {
//
//        public static final String OPC = "G";
//        public static final String TPC = "D";
//    }

    public static final class TransType {

        public static final String ILA = "ILA";
        public static final String DGE = "DGE";
        public static final String SITE_MODEL_1 = "SITE_MODEL_1";
        public static final String SITE_MODEL_2 = "SITE_MODEL_2";
        public static final String SITE_MODEL_2_FLEX = "SITE_MODEL_2_FLEX";
        public static final String SITE_MODEL_3 = "SITE_MODEL_3";
        public static final String SITE_MODEL_4 = "SITE_MODEL_4";
        public static final String SITE_MODEL_4_FLEX = "SITE_MODEL_4_FLEX";
        public static final String SITE_MODEL_5 = "SITE_MODEL_5";
        public static final String FLEX = "FLEX";
    }

    public static final class SiteType {

        public static final String ILA = "ILA";
        public static final String DGE = "DGE";
        public static final String SITE = "SITE";
    }

    public static final class RouteSite {

        public static final String START = "START";
        public static final String END = "END";
    }

    public static final class Gain {

        public static final BigDecimal LaunchPower = new BigDecimal(3);
        public static final BigDecimal TxPower = new BigDecimal(-1);
        public static final BigDecimal LcConnectorLoss = new BigDecimal(0.5);
        public static final BigDecimal MuxConnectorLoss = new BigDecimal(6);
        public static final BigDecimal MuxInsertionLoss96 = new BigDecimal(6);
        public static final BigDecimal MuxInsertionLoss64 = new BigDecimal(6);
        public static final BigDecimal MuxPanelInsertionLoss = new BigDecimal(0.8);
        public static final BigDecimal WssInsertionLoss = new BigDecimal(7);
        public static final BigDecimal CouplerInsertionLossAdd = new BigDecimal(3.5);
        public static final BigDecimal CouplerInsertionLossDrop = new BigDecimal(3.5);
        public static final BigDecimal OpInsertionLoss = new BigDecimal(1);
        public static final BigDecimal OpCouplerLossAdd = new BigDecimal(3.5);
        public static final BigDecimal MpoInsertionLoss = new BigDecimal(0.1);
        public static final BigDecimal OpsConnectorLoss = new BigDecimal(3);
        public static final BigDecimal voa = new BigDecimal(3);
        public static final BigDecimal MaxWssVoa = new BigDecimal(15);
        public static final BigDecimal MinWssVoa = new BigDecimal(0);
        public static final BigDecimal LPortMinPower = new BigDecimal(0);
    }

    public static final class Tunnel {

        public static String DWDM = "DWDM";
        public static String ROUTE = "A";
        public static String TUNNEL_100GE = "H";
        public static String TUNNEL_200GE = "H";
        public static String TUNNEL_400GE = "H";
    }

    public static final class EquipmentType {

        public static final String OA = "OA";
        public static final String ILA = "ILA";
        public static final String WSS = "WSS";
        public static final String OP = "OP";
        public static final String MUX = "MUX";
        public static final String MUX96 = "MUX96";
        public static final String MUX_96 = "MUX-96";
        public static final String MUX64 = "MUX64";
        public static final String CMUX64 = "CMUX64";
        public static final String CMUX_64 = "CMUX64";
        public static final String MUX48 = "MUX48";
        public static final String OT = "OT";
        public static final String PANEL = "PANEL";
        public static final String MUXPANEL = "MUXPANEL";
        public static final String MUX_PANEL = "MUXPANEL";
        public static final String CHASSIS = "CHASSIS";
        public static final String PSU = "PSU";
        public static final String FAN = "FAN";
        public static final String CU = "CU";
        public static final String OTHER = "Other";
        public static final String TRANSCEIVER = "TRANSCEIVER";
        public static final String AUTO = "AUTO";
        public static final String T2X2C4 = "200G-C4L2";
        public static final String T2X4C8 = "T2X4C8";
        public static final String T2X4C8MX = "T2X4C8MX";
        public static final String T2X6C12 = "T2X6C12";
        public static final String EMPTY = "EMPTY";
    }

    public static final class Namespace {

        public static final String SITE = "SITE";
        public static final String PHYSICAL = "PHYSICAL";
        public static final String OCH = "OCH";
        public static final String TUNNEL = "TUNNEL";
    }

    public static final class Reboot {

        public static final String WarmReboot = "warm-reboot";
        public static final String ColdReboot = "cold-reboot";
    }

    public static final class RebootState {

        public static final String start = "start";
        public static final String rebooting = "rebooting";
    }

    public static final class NeType {

        public static final String OT = "OT";
    }


    public static final class LinkModel {

        public static final String MODEL_1 = "1";
        public static final String MODEL_2 = "2";
        public static final String MODEL_2_FLEX = "2_FLEX";
        public static final String MODEL_3 = "3";
        public static final String MODEL_4 = "4";
        public static final String MODEL_4_FLEX = "4_FLEX";
        public static final String MODEL_5 = "5";
    }


    public static final class Property {

        public static final String Impl = "Impl";
        public static final String DeImpl = "DeImpl";
        public static final String Pending = "Pending";
        public static final String Failure = "Failure";
        public static final String Success = "Success";
        public static final String ErrorMsg = "ErrorMsg";
    }

    public static final class OtsLink {

        public static final String MainCn = "主用";
        public static final String SpareCn = "备用";
        public static final String MainEn = "Main";
        public static final String SpareEn = "Spare";
    }


//    public static final class OmsSplitting {
//
//        public static String neType = "opc";
//        public static String nePort = "INTERFACE-1-LOOPBACK";
//    }
}
