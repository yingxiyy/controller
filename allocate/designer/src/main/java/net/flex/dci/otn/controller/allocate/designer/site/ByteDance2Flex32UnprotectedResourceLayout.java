package net.flex.dci.otn.controller.allocate.designer.site;

/** Bone2.0 unprotected flexible-grid 32-channel physical slot contract. */
final class ByteDance2Flex32UnprotectedResourceLayout
        extends ByteDance2DefaultResourceLayout {

    @Override
    Integer getMainNodeSlot(String cardType, int occurrence) {
        if (occurrence != 0) {
            return null;
        }
        if ("TILA".equals(cardType)) {
            return 1;
        }
        if ("FMUX_32".equals(cardType)) {
            return 3;
        }
        return null;
    }
}
