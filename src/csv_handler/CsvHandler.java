package csv_handler;

import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.Scanner;

public class CsvHandler {
    private int sizeOfCSV;
    private String fileName = "randomNumbers.csv";

    public CsvHandler(int sizeOfCSV){
        this.sizeOfCSV = sizeOfCSV;
    }


    public void generateFile(){
        try {
            createFile();
            fillFile();

        } catch (Exception err){
            throw new Error(err);
        }
    }

    private void createFile(){
        try{
            File csvFile = new File(String.format("%s", this.fileName));
            if (csvFile.createNewFile()) {
                System.out.println("File created: " + csvFile.getName());
            }
        } catch (Exception err){
            throw new Error(err);
        }
    }

    private void fillFile(){
        String[] stringArray = new String[sizeOfCSV];
        Random rnd = new Random();

        for (int currentString = 0; currentString < sizeOfCSV; currentString++){
            stringArray[currentString] = Integer.toString(rnd.nextInt(0, 100));
        }

        String joinedString = String.join(", ", stringArray);
        writeInFile(joinedString);
    }

    private void writeInFile(String data){
        try {
            FileWriter writer = new FileWriter(String.format("%s", this.fileName));
            writer.write(data);
            writer.close();
        } catch (Exception err){
            throw new Error(err);
        }
    }

    public String[] readFile(){
        try{
            File csvFile = new File(this.fileName);
            Scanner reader = new Scanner(csvFile);

            String[] data = {};
            if(reader.hasNextLine()) {
                data = reader.nextLine().split(", ");
            }

            reader.close();
            return data;
        }catch (Exception err){
            throw new Error(err);
        }
    }

    public static String[] readFile(String file){
        try{
            File csvFile = new File(file);
            Scanner reader = new Scanner(csvFile);

            String[] data = {};
            if(reader.hasNextLine()) {
                data = reader.nextLine().split(", ");
            }

            reader.close();
            return data;
        }catch (Exception err){
            throw new Error(err);
        }
    }
}
