package net.flex.dci.otn.controller.allocate.designer.tunnel;

/** Preserves the existing commercial-C OCHP M/D selection behavior. */
class DefaultOchpResourceLayout implements OchpResourceLayout {

    @Override
    public boolean allowsMdPort(String portName) {
        return true;
    }
}
