package visuals;

import client.HostSpecs;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
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
        frame.setVisible(true);
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
        int rowCount = model.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            model.removeRow(i);
        }

        for (HostSpecs hostInfo : data) {
            model.addRow(
                    new Object[]{
                            "192.168.0.1",
                            hostInfo.processorModel,
                            hostInfo.processorSpeed,
                            hostInfo.numCores,
                            hostInfo.processorUsage,
                            hostInfo.diskCapacity,
                            hostInfo.RAMUsed,
                            hostInfo.osVersion,
                            "99999"
                    }
            );
        }
    }
}
