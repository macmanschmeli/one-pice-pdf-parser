import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import java.awt.geom.Area;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChapterDownloader implements Runnable {
    private final URL url;
    private final int chapter;
    private final String directoryPath;
    private final CommandLineProcessBar commandLineProcessBar;
    public ChapterDownloader(URL url, int chapter, String directoryPath, CommandLineProcessBar commandLineProcessBar) {
        this.url = url;
        this.chapter = chapter;
        this.directoryPath = directoryPath;
        this.commandLineProcessBar = commandLineProcessBar;
    }

    @Override
    public void run() {
        com.itextpdf.text.Document itextDocument = null;
        try {
            Rectangle rect= new Rectangle(1200,1700);
            itextDocument = new com.itextpdf.text.Document(rect, 20, 20, 20, 20);
            SaveChapterAsPdf(itextDocument);
            commandLineProcessBar.setDone();
        } catch (IOException | DocumentException e) {
            commandLineProcessBar.setErrors("Processing of Chapter "+chapter+" failed: "+e.getMessage());
            Path filepath=Paths.get(directoryPath,"OnePieceChapter_"+Integer.toString(chapter)+".pdf");
            if (Files.exists(filepath)) {
                try {
                    itextDocument.close();
                    Files.delete(filepath);
                } catch (IOException ex) {
                    commandLineProcessBar.setErrors("Processing of Chapter "+chapter+" failed: "+e.getMessage()+" | "+ex.getMessage());
                }
            }
        }
    }
    private void SaveChapterAsPdf(com.itextpdf.text.Document itextDocument) throws IOException, DocumentException {
        commandLineProcessBar.setPraeamble(chapter);
        commandLineProcessBar.setActive();
        InputStream input = url.openStream();
        Document document = CreateQuietTidy().parseDOM(input, new ByteArrayOutputStream());
        NodeList imgs = document.getElementsByTagName("img");
        List<String> srcs = new ArrayList<String>();
        if (imgs.getLength()<=0)return;

        for (int i = 0; i < imgs.getLength(); i++) {
            srcs.add(imgs.item(i).getAttributes().getNamedItem("src").getNodeValue());
        }
        //Path filepath=Paths.get("C:","Users","schme","Downloads","pics","OnePieceChapter_"+Integer.toString(chapter)+".pdf");
        Path filepath= Paths.get(directoryPath,"OnePieceChapter_"+Integer.toString(chapter)+".pdf");
        PdfWriter.getInstance(itextDocument, new FileOutputStream(filepath.toString()));
        itextDocument.open();
        int current=0;
        for (String src: srcs) {

            PrintProgressBar(current * 100 / srcs.size());

            while (true) {
                try {
                    SaveImageToPdf(itextDocument, new URL(src));
                    break;
                }catch (MalformedURLException ex){
                    itextDocument.newPage();
                    Font f = new Font(Font.FontFamily.TIMES_ROMAN, 25.0f, Font.BOLD, BaseColor.BLACK);
                    Chunk c = new Chunk("sorry, the server has no image here. See for yourself on: "+url.toString(), f);
                    c.setBackground(BaseColor.RED);
                    Paragraph p1 = new Paragraph(c);
                    p1.setAlignment(Paragraph.ALIGN_CENTER);
                    itextDocument.add(p1);
                    commandLineProcessBar.setErrors("Exception during Image Download: link to picture is missing");
                    break;
                }
                catch (IOException ex){
                    commandLineProcessBar.setErrors("Exception during Image Download: "+ex.getMessage());
                }
            }
            current++;
        }
        itextDocument.close();
        PrintProgressBar(100);
    }
    private void PrintProgressBar(int percent){
        String string =
                String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")) +
                String.format(" %d%% [", percent) +
                "\u001B[33m" +
                String.join("", Collections.nCopies(percent, "=")) +
                '>' +
                String.join("", Collections.nCopies(100 - percent, " ")) +
                "\u001B[0m" +
                ']';
        commandLineProcessBar.setProcessbar(string);
    }
    private void SaveImageToPdf(com.itextpdf.text.Document document,URL url) throws IOException, DocumentException {
        Image image = Image.getInstance(url);
        float imageHeight =image.getHeight();
        float documentHeight= document.getPageSize().getHeight();
        int margins= 100;
        if (imageHeight>documentHeight){
            float scalepersentage=(documentHeight-margins)/imageHeight;
            image.scalePercent(scalepersentage*100);
        }
        float imageWidth =image.getWidth();
        float documentWidth= document.getPageSize().getWidth();
        if (imageWidth>documentWidth){
            float scalepersentage=(documentWidth-margins)/imageWidth;
            image.scalePercent(scalepersentage*100);
        }
        document.add(image);
    }
    private Tidy CreateQuietTidy(){
        Tidy tidy= new Tidy();
        ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
        PrintWriter dummyOut = new PrintWriter(outputStream);
        tidy.setErrout(dummyOut);
        return tidy;
    }
}
