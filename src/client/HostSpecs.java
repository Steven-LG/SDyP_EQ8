package client;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.List;

public class HostSpecs {
    public final SystemInfo systemInfo = new SystemInfo();
    public final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    public final CentralProcessor processor = hardware.getProcessor();
    public String processorModel;
    public double processorSpeed;
    public int numCores;
    public long diskCapacity;

    private HWDiskStore[] diskStores;

    public String osVersion;

    public double processorUsage;

    public List<Disk> disks;

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
    private void getCurrentUsage(){
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
}
