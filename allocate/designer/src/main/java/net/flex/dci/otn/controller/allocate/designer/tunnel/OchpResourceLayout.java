package net.flex.dci.otn.controller.allocate.designer.tunnel;

/** Defines the M/D resources available to one OCHP route leg. */
interface OchpResourceLayout {

    boolean allowsMdPort(String portName);
}
