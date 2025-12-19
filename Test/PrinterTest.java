import org.junit.Test;

import java.io.IOException;

public class PrinterTest {
    @Test
    public void PrintTest() throws IOException {
        System.out.println("hi");
        System.out.println("new line");
        System.out.println("bye");
        //new ProcessBuilder("dir").start();
        //System.out.print("\033[H\033[2J");
        for (int i = 0; i < 20; i++) {
            System.out.print("\f");

        }
        //Runtime.getRuntime().exec("cls");
        System.out.println("it");
    }
}
