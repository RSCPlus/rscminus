package rscminus.scraper;

public class ReplayMetadata {
    public int replayLength;
    public long dateModified;
    public int IPAddress1; // for ipv6. 0 if ipv4
    public int IPAddress2; // for ipv6. 0 if ipv4
    public int IPAddress3; // for ipv6. 0xFFFF if ipv4
    public int IPAddress4; // for ipv4 or ipv6
    public byte conversionSettings;
    public int userField;
}
