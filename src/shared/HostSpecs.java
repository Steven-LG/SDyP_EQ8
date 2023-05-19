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
import java.util.List;

public class HostSpecs implements Serializable {
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

    public transient List<Disk> disks;

    public long totalRAM;

    public long availableRAM;

    public long RAMUsed;


    public HostSpecs(){
        getClientStaticInfo();
        getCurrentUsage();
    }

    private void getClientStaticInfo(){
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
        disks = new ArrayList<Disk>();

        for (HWDiskStore diskStore : diskStores) {
            disks.add(
                    new Disk(
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

    @Override
    public String toString() {
        return "HostSpecs{" +
                "systemInfo=" + systemInfo +
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
}