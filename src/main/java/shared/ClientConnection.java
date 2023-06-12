package shared;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
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

    public String strDiskCapacity;

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

        strDiskCapacity = String.valueOf(diskCapacity);
        int endIndex = strDiskCapacity.length() - 6; // Calculate the index to end the substring
        strDiskCapacity = strDiskCapacity.substring(0, endIndex) + " MB";
    }
    public void getCurrentUsage(){
        processorUsage = hardware.getProcessor().getSystemCpuLoad(0) * 100;

        //Get CPU usage
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            double cpuUsage = sunOsBean.getSystemCpuLoad() * 100;
            processorUsage = cpuUsage;
        } else {
            System.out.println("Unsupported operating system.");
        }

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

    public int getRank(ClientConnection cConnection){
        double modelScore = 0.0d;
        double sOScore = 0.0d;
        double genScore = 0.0d;
        // Define regex patterns for different processor models
        Pattern i9Pattern = Pattern.compile("i9", Pattern.CASE_INSENSITIVE);
        Pattern i7Pattern = Pattern.compile("i7", Pattern.CASE_INSENSITIVE);
        Pattern i5Pattern = Pattern.compile("i5", Pattern.CASE_INSENSITIVE);
        Pattern i3Pattern = Pattern.compile("i3", Pattern.CASE_INSENSITIVE);
        Pattern CeleronPattern = Pattern.compile("Celeron", Pattern.CASE_INSENSITIVE);
        Pattern r7Pattern = Pattern.compile("Ryzen 7", Pattern.CASE_INSENSITIVE);
        Pattern r5Pattern = Pattern.compile("Ryzen 5", Pattern.CASE_INSENSITIVE);
        Pattern r3Pattern = Pattern.compile("Ryzen 3", Pattern.CASE_INSENSITIVE);

        // Define regex patterns for different SO models
        Pattern Win11Pattern = Pattern.compile("11", Pattern.CASE_INSENSITIVE);
        Pattern Win10Pattern = Pattern.compile("10", Pattern.CASE_INSENSITIVE);

        // Define regex patterns for different processor generations
        Pattern g11Pattern = Pattern.compile("-11", Pattern.CASE_INSENSITIVE);
        Pattern g10Pattern = Pattern.compile("-10", Pattern.CASE_INSENSITIVE);
        Pattern g9Pattern = Pattern.compile("-9", Pattern.CASE_INSENSITIVE);
        Pattern g8Pattern = Pattern.compile("-8", Pattern.CASE_INSENSITIVE);
        Pattern g7Pattern = Pattern.compile("-7", Pattern.CASE_INSENSITIVE);
        Pattern g6Pattern = Pattern.compile("-6", Pattern.CASE_INSENSITIVE);
        Pattern g5Pattern = Pattern.compile("-5", Pattern.CASE_INSENSITIVE);
        Pattern rM99Pattern = Pattern.compile("9 9", Pattern.CASE_INSENSITIVE);
        Pattern rM97Pattern = Pattern.compile("9 7", Pattern.CASE_INSENSITIVE);
        Pattern rM79Pattern = Pattern.compile("7 9", Pattern.CASE_INSENSITIVE);
        Pattern rM77Pattern = Pattern.compile("7 7", Pattern.CASE_INSENSITIVE);
        Pattern rM73Pattern = Pattern.compile("7 3", Pattern.CASE_INSENSITIVE);

        // Match the processor name against the regex patterns
        Matcher i9Matcher = i9Pattern.matcher(this.processorModel);
        Matcher i7Matcher = i7Pattern.matcher(this.processorModel);
        Matcher i5Matcher = i5Pattern.matcher(this.processorModel);
        Matcher i3Matcher = i3Pattern.matcher(this.processorModel);
        Matcher CeleronMatcher = CeleronPattern.matcher(this.processorModel);
        Matcher r7Matcher = r7Pattern.matcher(this.processorModel);
        Matcher r5Matcher = r5Pattern.matcher(this.processorModel);
        Matcher r3Matcher = r3Pattern.matcher(this.processorModel);

        // Match the SO name against the regex patterns
        Matcher Win11Matcher = Win11Pattern.matcher(this.osVersion);
        Matcher Win10Matcher = Win10Pattern.matcher(this.osVersion);

        // Match the processor model against the regex patterns
        Matcher g11Matcher = g11Pattern.matcher(this.processorModel);
        Matcher g10Matcher = g10Pattern.matcher(this.processorModel);
        Matcher g9Matcher = g9Pattern.matcher(this.processorModel);
        Matcher g8Matcher = g8Pattern.matcher(this.processorModel);
        Matcher g7Matcher = g7Pattern.matcher(this.processorModel);
        Matcher g6Matcher = g6Pattern.matcher(this.processorModel);
        Matcher g5Matcher = g5Pattern.matcher(this.processorModel);
        Matcher rM99Matcher = rM99Pattern.matcher(this.processorModel);
        Matcher rM97Matcher = rM97Pattern.matcher(this.processorModel);
        Matcher rM79Matcher = rM79Pattern.matcher(this.processorModel);
        Matcher rM77Matcher = rM77Pattern.matcher(this.processorModel);
        Matcher rM73Matcher = rM73Pattern.matcher(this.processorModel);

        // Check which pattern matches and set the model accordingly
        if (i9Matcher.find()) {
            //i9 Model
            modelScore = 2300;
        } else if (i7Matcher.find()) {
            //i7 Model
            modelScore = 2000;
        } else if (i5Matcher.find()) {
            //i5 Model
            modelScore = 1800;
        } else if (i3Matcher.find()) {
            //i3 Model
            modelScore = 1300;
        } else if (CeleronMatcher.find()) {
            //Celeron Model
            modelScore = 250;
        } else if (r7Matcher.find()) {
            //Ryzen 7 Model
            modelScore = 2400;
        } else if (r7Matcher.find()) {
            //Ryzen 5 Model
            modelScore = 2100;
        } else if (r7Matcher.find()) {
            //Ryzen 3 Model
            modelScore = 1800;
        } else {
            modelScore = 1000;
        }

        // Check which pattern matches and set the SO accordingly
        if (Win11Matcher.find()) {
            //Windows 11 SO
            sOScore = 1800;
        } else if (Win10Matcher.find()) {
            //Windows 10 SO
            sOScore = 2000;
        } else {
            sOScore = 1000;
        }

        // Check which pattern matches and set the model accordingly
        if (g11Matcher.find()) {
            //Generation 11 for intel
            genScore = 2300;
        } else if (g10Matcher.find()) {
            //Generation 10 for intel
            genScore = 2000;
        } else if (g9Matcher.find()) {
            //Generation 9 for intel
            genScore = 1800;
        } else if (g8Matcher.find()) {
            //Generation 8 for intel
            genScore = 1600;
        } else if (g7Matcher.find()) {
            //Generation 7 for intel
            genScore = 1500;
        } else if (g6Matcher.find()) {
            //Generation 6 for intel
            genScore = 1300;
        } else if (g5Matcher.find()) {
            //Generation 5 for intel
            genScore = 1200;
        } else if (rM99Matcher.find()) {
            //Model 9 generation 9 for AMD
            genScore = 2900;
        } else if (rM97Matcher.find()) {
            //Model 9 generation 7 for AMD
            genScore = 2700;
        } else if (rM79Matcher.find()) {
            //Model 7 generation 9 for AMD
            genScore = 2600;
        } else if (rM77Matcher.find()) {
            //Model 7 generation 7 for AMD
            genScore = 2500;
        } else if (rM73Matcher.find()) {
            //Model 7 generation 3 for AMD
            genScore = 2400;
        } else {
            genScore = 1000;
        }
        //Assigning dinamic score values
        int processorDisponibility = (int) processorUsage - 100;
        int scoreProcessor = (int) ((processorDisponibility * 10) * (processorSpeed / 1000) * numCores) * 10 + (int) modelScore + (int) genScore * -1;
        int scoreRAM = (int) (this.availableRAM/3000000) * 10;
        int oSScore = (int) sOScore;
        //int rankScore = (int) (this.RAMUsed * this.availableRAM * this.processorSpeed * modelScore) / 100000;

        //int rankScore = scoreRAM + scoreProcessor * -1;
        int rankScore = scoreProcessor * -1 + scoreRAM + oSScore;
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
