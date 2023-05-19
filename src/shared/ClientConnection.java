package shared;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientConnection implements Serializable, Comparable<ClientConnection> {
    public String ipAddress;

    public double rank = 0;
    public byte[] fileBytes;
    public HostSpecs hostSpecs;

    public Date timer = new Date(System.currentTimeMillis());

    public boolean firstConnection;

    public ClientConnection(byte[] fileBytes) {
        this.fileBytes = fileBytes;

        getClientStaticInfo();
        getCurrentUsage();
    }
    public ClientConnection(){}

    @Override
    public String toString() {
        return "ClientConnection{" +
                "fileBytes=" + Arrays.toString(fileBytes) +
                ", hostSpecs=" + hostSpecs +
                ", timer=" + timer +
                ", firstConnection=" + firstConnection +
                ", systemInfo=" + systemInfo +
                ", hardware=" + hardware +
                ", processor=" + processor +
                ", processorModel='" + processorModel + '\'' +
                ", processorSpeed=" + processorSpeed +
                ", numCores=" + numCores +
                ", diskCapacity=" + diskCapacity +
                ", diskStores=" + Arrays.toString(diskStores) +
                ", osVersion='" + osVersion + '\'' +
                ", processorUsage=" + processorUsage +
                ", disks=" + disks +
                ", totalRAM=" + totalRAM +
                ", availableRAM=" + availableRAM +
                ", RAMUsed=" + RAMUsed +
                '}';
    }

    @Override
    public int compareTo(ClientConnection other) {
        return Double.compare(this.rank, other.rank);
    }

    public transient final SystemInfo systemInfo = new SystemInfo();
    public transient final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    public transient final CentralProcessor processor = hardware.getProcessor();
    public String processorModel;
    public double processorSpeed;
    public int numCores;
    public long diskCapacity;

    private transient HWDiskStore[] diskStores;

    public String osVersion;

    public double processorUsage;

    public transient List<HostSpecs.Disk> disks;

    public long totalRAM;

    public long availableRAM;

    public long RAMUsed;

    public void getClientStaticInfo(){
        processorModel = processor.getProcessorIdentifier().getName();
        processorSpeed = processor.getProcessorIdentifier().getVendorFreq() / 1e6;
        numCores = processor.getPhysicalProcessorCount();

        OperatingSystem os = systemInfo.getOperatingSystem();
        osVersion = String.valueOf(os.getVersionInfo());

        HWDiskStore[] diskStores = hardware.getDiskStores().toArray(new HWDiskStore[0]);
        diskCapacity = diskStores.length > 0 ? diskStores[0].getSize() : 0;
    }
    public void getCurrentUsage(){
        processorUsage = hardware.getProcessor().getSystemCpuLoad(0) * 100;

        diskStores = hardware.getDiskStores().toArray(new HWDiskStore[0]);
        disks = new ArrayList<HostSpecs.Disk>();

        for (HWDiskStore diskStore : diskStores) {
            disks.add(
                    new HostSpecs.Disk(
                            diskStore.getName(),
                            diskStore.getCurrentQueueLength(),
                            diskStore.getWrites(),
                            diskStore.getSize()
                    )
            );
        }

        GlobalMemory memory = hardware.getMemory();
        totalRAM = memory.getTotal();
        availableRAM = memory.getAvailable();
        RAMUsed = totalRAM - availableRAM;
    }

    public static int getRank(ClientConnection cConnection){
        double generationScore = 0.0d;
        // Define regex patterns for different processor generations
        Pattern i9Pattern = Pattern.compile("i9", Pattern.CASE_INSENSITIVE);
        Pattern i7Pattern = Pattern.compile("i7", Pattern.CASE_INSENSITIVE);
        Pattern i5Pattern = Pattern.compile("i5", Pattern.CASE_INSENSITIVE);
        Pattern i3Pattern = Pattern.compile("i3", Pattern.CASE_INSENSITIVE);

        // Match the processor name against the regex patterns
        Matcher i9Matcher = i9Pattern.matcher(cConnection.processorModel);
        Matcher i7Matcher = i7Pattern.matcher(cConnection.processorModel);
        Matcher i5Matcher = i5Pattern.matcher(cConnection.processorModel);
        Matcher i3Matcher = i3Pattern.matcher(cConnection.processorModel);

        // Check which pattern matches and set the generation accordingly
        if (i9Matcher.find()) {
            //9th Generation
            generationScore = 900;

        } else if (i7Matcher.find()) {
            //8th Generation
            generationScore = 800;

        } else if (i5Matcher.find()) {
            //7th Generation
            generationScore = 700;

        } else if (i3Matcher.find()) {
            //6th Generation
            generationScore = 500;

        }
        int rankScore = (int) (cConnection.RAMUsed * cConnection.availableRAM * cConnection.processorSpeed * generationScore) / 100000;

        return rankScore;
    }

    public static class Disk{
        public String name;
        public double usage;
        public long freeSpace;
        public long size;

        public Disk(String name, double usage, long freeSpace, long size) {
            this.name = name;
            this.usage = usage;
            this.freeSpace = freeSpace;
            this.size = size;
        }
    }
}
