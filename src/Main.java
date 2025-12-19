import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws IOException, DocumentException, InterruptedException {
        if (args.length!=3)throw new IllegalArgumentException("Wrong number of arguments");
        String path= args[0];
        int start=Integer.parseInt(args[1]);
        int stop= Integer.parseInt(args[2]);
        DownloadChapters(start,stop,path);
    }
    private static StringBuilder builder= new StringBuilder();
    private static SortedSet<Integer> doneChapters= new TreeSet<>();
    public static void DownloadChapters(int start,int stop,String path) throws IOException, DocumentException, InterruptedException {
        try(Stream<Path> files=Files.list(Paths.get(path))) {
            List<Path> OPfiles= files.toList();
            InputStream input = new URL("https://w061.1piecemanga.com/").openStream();
            Document document = CreateQuietTidy().parseDOM(input, new ByteArrayOutputStream());
            NodeList chapterLinks = document.getElementsByTagName("a");
            List<String> srcs = new ArrayList<String>();
            for (int i = 0; i < chapterLinks.getLength(); i++) {
                srcs.add(chapterLinks.item(i).getAttributes().getNamedItem("href").getNodeValue());
            }
            Pattern pattern = Pattern.compile(".*one-piece-chapter-(?<chapterId>\\d+).*");

            ThreadPoolExecutor executorService= (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            List<CommandLineProcessBar>commandLineProcessBars=new ArrayList<>();
            List<Future> futures= new ArrayList<>();
            for (String src : srcs) {
                Matcher matcher = pattern.matcher(src);
                if (matcher.matches()) {
                    int chapter = Integer.parseInt(matcher.group("chapterId"));
                    Pattern chapterPattern = Pattern.compile(".*OnePieceChapter_" + chapter + ".*");
                    if (chapter >= start && chapter <= stop) {
                        if (OPfiles.stream().noneMatch(n -> chapterPattern.matcher(n.toString()).matches())) {
                            //SaveChapterAsPdf(new URL(src), chapter, path);
                            CommandLineProcessBar commandLineProcessBar= new CommandLineProcessBar();
                            futures.add(executorService.submit(new ChapterDownloader(new URL(src),chapter,path,commandLineProcessBar)));
                            commandLineProcessBars.add(commandLineProcessBar);
                        } else {
                            builder.append("Chapter "+chapter+" already existant in folder\n");
                        }
                    }
                }
            }
            while (executorService.getActiveCount()!=0) {
                Thread.sleep(1000);
                PrintProcessBars(commandLineProcessBars);
            }
            PrintProcessBars(commandLineProcessBars);
            executorService.shutdown();
            System.out.println("Done");
        }
    }
    private static void PrintProcessBars(List<CommandLineProcessBar> commandLineProcessBars) throws IOException {
        System.out.println("\u001B[H\u001B[2J");
        //System.out.println(builder);
        System.out.print("finished downloading chapters: ");
        for (Integer num : doneChapters) {
            System.out.print(num+"; ");
        }
        System.out.println();
        int i=0;
        for (CommandLineProcessBar bar : commandLineProcessBars) {
            if (i>15)break;
            if (bar.IsActive()&& !bar.IsDone()) {
                System.out.println(bar.getOutput());
                i++;
            }
            if (bar.IsDone()&&!doneChapters.contains(bar.getChapter())) {
                doneChapters.add(bar.getChapter());
            }
        }
    }
    public static Tidy CreateQuietTidy(){
        Tidy tidy= new Tidy();
        ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
        PrintWriter dummyOut = new PrintWriter(outputStream);
        tidy.setErrout(dummyOut);
        return tidy;
    }
}
