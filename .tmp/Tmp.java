import java.nio.file.*;
import main.java.networktool.gui.panels.tags.*;
public class Tmp {
  public static void main(String[] args) throws Exception {
    Path tmp = Files.createTempDirectory("hosttagtest");
    HostTagStore store = HostTagStore.getInstance();
    store.setDataDir(tmp);
    store.addTag("4.4.4.4", "persisted");
    store.setFavorite("4.4.4.4", true);
    System.out.println(Files.readString(tmp.resolve("hostTags.json")));
    store.setDataDir(tmp);
    System.out.println(store.getTags("4.4.4.4"));
    System.out.println(store.isFavorite("4.4.4.4"));
  }
}
