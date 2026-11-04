package org.cloudsimplus.examples.hillclimbing;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicyRoundRobin;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.provisioners.PeProvisionerSimple;
import org.cloudsimplus.provisioners.ResourceProvisionerSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HillClimbingExp {

    private static final int NUM_DCS = 1;
    private static final int HOSTS_PER_DC = 1;
    private static final int VMS_PER_HOST = 3;
    private static final int TOTAL_VMS = NUM_DCS * HOSTS_PER_DC * VMS_PER_HOST;
    private static final long SEED = 42;

    public static void main(String[] args) {
        System.out.println("=== Menjalankan Simulasi Hill-Climbing di Azure ===");
        
        int jumlahCloudlet = 100; 
        
        System.out.println("Memulai simulasi Hill-Climbing...");
        double[] hasil = runSimulation(jumlahCloudlet, "Hill-Climbing");
        
        System.out.println("\n=== HASIL SIMULASI KESELURUHAN ===");
        System.out.printf("Makespan Global : %.2f detik\n", hasil[0]);
        System.out.printf("Total Cost      : $%.4f\n", hasil[1]);
    }
    
    public static double[] runSimulation(int numCloudlets, String algorithmName) {
        CloudSimPlus simulation = new CloudSimPlus();
        List<DatacenterSimple> datacenters = new ArrayList<>();

        for (int i = 0; i < NUM_DCS; i++) {
            datacenters.add(createDatacenter(simulation, "DC-" + (i + 1)));
        }

        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        final int[] lastDcIndex = {-1};
        broker.setDatacenterMapper((lastDatacenter, vm) -> {
            if (lastDcIndex[0] == -1) {
                lastDcIndex[0] = 0;
                return datacenters.get(0);
            }
            lastDcIndex[0] = (lastDcIndex[0] + 1) % datacenters.size();
            return datacenters.get(lastDcIndex[0]);
        });

        List<VmSimple> vmList = new ArrayList<>();
        for (int i = 0; i < TOTAL_VMS; i++) {
            vmList.add(createVm(i));
        }
        broker.submitVmList(vmList);

        List<CloudletSimple> cloudletList = createCloudlets("tugas.csv");

        // --- PEMILIHAN ALGORITMA ---
        List<Cloudlet> optimizedOrder;
        if (algorithmName.equals("FCFS")) {
            FcfsScheduler fcfs = new FcfsScheduler();
            optimizedOrder = fcfs.solve(new ArrayList<>(cloudletList), new ArrayList<>(vmList));
        } else if (algorithmName.equals("Random")) {
            RandomScheduler rand = new RandomScheduler();
            optimizedOrder = rand.solve(new ArrayList<>(cloudletList), new ArrayList<>(vmList));
        } else {
            HillClimbingScheduler hc = new HillClimbingScheduler(
                    new ArrayList<>(cloudletList),
                    new ArrayList<>(vmList),
                    SEED,
                    500, 
                    "2-exchange"
            );
            optimizedOrder = hc.solve();
        }
        // ---------------------------

        for (Cloudlet c : optimizedOrder) {
            broker.submitCloudlet(c);
        }

        simulation.start();

        // --- TAMBAHAN UNTUK PER-DC, PER-HOST, DAN PER-VM ---
        // Struktur data: Map<DC_Name, Map<Host_ID, List<Cloudlet>>>
        Map<String, Map<Integer, List<Cloudlet>>> cloudletsPerHost = new HashMap<>();
        
        for (Cloudlet c : cloudletList) {
            String dcName = c.getVm().getHost().getDatacenter().getName();
            int hostId = (int) c.getVm().getHost().getId();
            
            cloudletsPerHost.putIfAbsent(dcName, new HashMap<>());
            cloudletsPerHost.get(dcName).putIfAbsent(hostId, new ArrayList<>());
            cloudletsPerHost.get(dcName).get(hostId).add(c);
        }

        System.out.println("\n=== RINCIAN PER DATA CENTER DAN HOST ===");
        // Urutkan nama DC (DC-1, DC-2, DC-3)
        List<String> sortedDcNames = new ArrayList<>(cloudletsPerHost.keySet());
        sortedDcNames.sort(String::compareTo);

        for (String dcName : sortedDcNames) {
            System.out.println("--------------------------------------------------");
            System.out.printf("DATA CENTER: %s\n", dcName);
            
            Map<Integer, List<Cloudlet>> hostMap = cloudletsPerHost.get(dcName);
            // Urutkan Host ID
            List<Integer> sortedHostIds = new ArrayList<>(hostMap.keySet());
            sortedHostIds.sort(Integer::compareTo);
            
            for (Integer hostId : sortedHostIds) {
                List<Cloudlet> hostCloudlets = hostMap.get(hostId);
                double hostMakespan = calculateMakespan(hostCloudlets);
                double hostCost = hostMakespan * 0.01; 
                
                System.out.printf("  -> Host ID %d:\n", hostId);
                System.out.printf("     - Jumlah Tugas : %d\n", hostCloudlets.size());
                System.out.printf("     - Makespan     : %.2f detik\n", hostMakespan);
                System.out.printf("     - Total Cost   : $%.4f\n", hostCost);
                
                // (Opsional) Tampilkan VM di dalam Host ini
                // Map<Vm_ID, List<Cloudlet>>
                Map<Long, List<Cloudlet>> vmMap = new HashMap<>();
                for (Cloudlet c : hostCloudlets) {
                    long vmId = c.getVm().getId();
                    vmMap.putIfAbsent(vmId, new ArrayList<>());
                    vmMap.get(vmId).add(c);
                }
                
                List<Long> sortedVmIds = new ArrayList<>(vmMap.keySet());
                sortedVmIds.sort(Long::compareTo);
                
                for (Long vmId : sortedVmIds) {
                    List<Cloudlet> vmCloudlets = vmMap.get(vmId);
                    System.out.printf("       * VM-%d | Tugas: %d | Makespan VM: %.2f dtk\n", 
                        vmId, vmCloudlets.size(), calculateMakespan(vmCloudlets));
                }
            }
        }
        System.out.println("--------------------------------------------------");
        // ---------------------------

        double makespan = calculateMakespan(cloudletList);
        double totalCost = makespan * 0.01; 

        return new double[]{makespan, totalCost};
    }

    private static DatacenterSimple createDatacenter(CloudSimPlus simulation, String name) {
        List<HostSimple> hostList = new ArrayList<>();
        
        for (int i = 0; i < HOSTS_PER_DC; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                peList.add(new PeSimple(2600, new PeProvisionerSimple()));
            }
            
            HostSimple host = new HostSimple(6144, 1000000, 500000, peList);
            host.setRamProvisioner(new ResourceProvisionerSimple())
                .setBwProvisioner(new ResourceProvisionerSimple())
                .setVmScheduler(new VmSchedulerTimeShared());
            hostList.add(host);
        }
        
        DatacenterSimple dc = new DatacenterSimple(simulation, hostList, new VmAllocationPolicyRoundRobin());
        dc.setName(name);
        return dc;
    }

    private static VmSimple createVm(int id) {
        VmSimple vm = new VmSimple(id, 2600, 1);
        vm.setRam(2048)
          .setBw(1000)
          .setSize(50000)
          .setCloudletScheduler(new CloudletSchedulerTimeShared());
        return vm;
    }

    private static List<CloudletSimple> createCloudlets(String filePath) {
        List<CloudletSimple> list = new ArrayList<>();
        UtilizationModelFull utilization = new UtilizationModelFull();
        
        // Membaca file CSV
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                // Lewati baris header (id,panjang_mi)
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                // Pisahkan baris berdasarkan koma
                String[] values = line.split(",");
                if (values.length < 2) continue; // Abaikan baris kosong/rusak
                
                int id = Integer.parseInt(values[0].trim());
                long length = Long.parseLong(values[1].trim()); // Panjang dalam MI
                
                // Buat cloudlet baru sesuai data dari CSV
                CloudletSimple cloudlet = new CloudletSimple(id, length, 1);
                cloudlet.setFileSize(1000) // Ukuran file statis (bisa disesuaikan)
                        .setOutputSize(1000)
                        .setUtilizationModel(utilization);
                
                list.add(cloudlet);
            }
        } catch (IOException e) {
            System.err.println("Gagal membaca file CSV: " + e.getMessage());
            e.printStackTrace();
        }
        
        return list;
    }

    private static double calculateMakespan(List<? extends Cloudlet> cloudlets) {
        double max = 0;
        for (Cloudlet c : cloudlets) {
            max = Math.max(max, c.getFinishTime());
        }
        return max;
    }

    private static double calculateTotalCost(List<? extends Cloudlet> cloudlets) {
        double costPerSecond = 0.01;
        double totalExec = 0;
        for (Cloudlet c : cloudlets) {
            totalExec += (c.getFinishTime() - c.getStartTime());
        }
        return totalExec * costPerSecond;
    }

    private static double calculateAvgWaitingTime(List<? extends Cloudlet> cloudlets) {
        double totalWait = 0;
        for (Cloudlet c : cloudlets) {
            totalWait += (c.getStartTime() - c.getBrokerArrivalTime());
        }
        return totalWait / cloudlets.size();
    }
}
