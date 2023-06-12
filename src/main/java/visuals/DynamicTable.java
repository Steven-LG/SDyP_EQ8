package visuals;

import shared.HostSpecs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DynamicTable {
    public List<HostSpecs> registers;
    public DefaultTableModel columns;

    public JTable table;
    public JFrame frame;
    public DynamicTable(){
        registers = new ArrayList<>();
        columns = new DefaultTableModel();
        table = new JTable(columns);
        frame = new JFrame("Tabla Dinámica");
        JScrollPane scrollPane = new JScrollPane(table);
        frame.getContentPane().add(scrollPane);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void initializeTable () {
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(2000);

                    updateTableModel(columns, registers);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void updateTableModel(DefaultTableModel model, List<HostSpecs> data) {
        Collections.sort(data,new Comparator<HostSpecs>() {
            @Override
            public int compare(HostSpecs c1, HostSpecs c2) {
                return Double.compare(c2.rank, c1.rank);
            }
        });

        model.setRowCount(0);

        for (HostSpecs hostInfo : data) {
            model.addRow(
                    new Object[]{
                            hostInfo.ipAddress,
                            hostInfo.processorModel,
                            hostInfo.processorSpeed + " GHz",
                            hostInfo.numCores,
                            (int) Math.floor(hostInfo.processorUsage)+ "%",
                            hostInfo.strDiskCapacity,
                            hostInfo.strRAMUsed,
                            hostInfo.osVersion,
                            hostInfo.rank,
                            hostInfo.timer
                    }
            );
        }

        model.fireTableDataChanged();
    }
}
