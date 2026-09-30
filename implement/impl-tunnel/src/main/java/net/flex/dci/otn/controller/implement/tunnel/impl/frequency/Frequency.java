package net.flex.dci.otn.controller.implement.tunnel.impl.frequency;

import net.flex.dci.otc.common.util.MuxCardPortFormatting;
import net.flex.dci.otc.common.util.NeYangModel;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Frequency {
  String formattedFrequency;

  String chNumber;
  String lowerFrequecy;
  String upperFrequecy;
  BigInteger centFreq;

  String muxChannelPortRegex = null;
  String muxChannelPortFormat = null;

  //the format is channelID:lowerFrequecy,higherFrequecy.
  public Frequency(NeYangModel model, String frequency) {
    formattedFrequency = frequency;

    String tmp[] = frequency.split("-");
    chNumber = tmp[0];
    String lFqcy[] = tmp[1].split(",");
    lowerFrequecy = lFqcy[0];
    upperFrequecy = lFqcy[1];
    BigInteger lowFreq = new BigInteger(lowerFrequecy);
    BigInteger uppFreq = new BigInteger(upperFrequecy);
    centFreq = getCentFreq(lowFreq, uppFreq);

    MuxCardPortFormatting formatting = new MuxCardPortFormatting(model);
    muxChannelPortRegex = formatting.getPortMatchingRegex();
    muxChannelPortFormat = formatting.getPortOutputFormat();

  }

  public String getFormattedFrequency() {
    return formattedFrequency;
  }

  public String getChNumber() {
    return chNumber;
  }

  public String getLowerFrequecy() {
    return lowerFrequecy;
  }

  public String getUpperFrequecy() {
    return upperFrequecy;
  }

  public BigInteger getCentFreq() {
    return centFreq;
  }

  public static BigInteger getCentFreq(BigInteger lowFreq, BigInteger uppFreq) {
    return uppFreq.add(lowFreq).divide(new BigInteger("2"));
  }

  public String getMuxChannelId () {
    return String.format(muxChannelPortFormat, chNumber, chNumber);
  }

  /**
   *  based on String (xcID) to check does this is related to Mux port
   * @param xcId
   * @return
   */
  public boolean hasMuxChannel(String xcId) {
    Matcher matcher = Pattern.compile(muxChannelPortRegex).matcher(xcId);
    return matcher.find();
  }

  /**
   *  based on String (xcID) to check does this is related to frequency
   * @param xcId
   * @return
   */
  public boolean hasFrequency(String xcId, BigInteger centFreq) {
    String key = String.format("/%s", centFreq.toString());
    return xcId.contains(key);
  }

  public String replaceMuxChannelTpId(String id) {
    String newId = String.format(muxChannelPortFormat, chNumber, chNumber);
    return id.replaceAll(muxChannelPortRegex, newId);
  }


  /**
   * frequencyXcID format look like
   *   XC-Site-1676343450314#Ne-1676343491673#MUX-1-50#PORT-1-50-M48D48/191400000-Site-1676343450314#Ne-1676343491673#MUX-1-50#PORT-1-50-MUXDMUX/191400000
   * here use regex to replace old frequency which in xcID with new one
   *   String xcFrequencyRegex = "/\\d+";
   * @param id
   * @return
   */
  public String replaceXCIdFrequency(String id) {
    String newId = String.format("/%s", centFreq.toString());
    String xcFrequencyRegex = "/\\d+";
    return id.replaceAll(xcFrequencyRegex, newId);
  }

}
