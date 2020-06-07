/*
Copyright (c) 2020 Divested Computing Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Scraper {

  public static void main(String[] args) {
    String link = args[1];
    if (link.startsWith("https://source.android.com/security/bulletin/")) {
      scrapeGASB(link);
    } else {
      System.out.println("Link unsupported");
    }
  }

  public static void scrapeGASB(String link) {
    try {
      URL url = new URL(link);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.connect();
      Scanner page = new Scanner(connection.getInputStream());
      String line = "";
      boolean searching = false;
      while (page.hasNextLine()) {
        line = page.nextLine();

        if (line.startsWith("href")) {
          line = "<a " + line;
        }

        if (line.contains("<a") && !line.contains("</a>")) {
          line += "</a>";
        }

        if (line.contains("<table>")) {
          searching = true;
        }
        if (line.contains("</table>")) {
          searching = false;
        }
        if (searching) {
          if (line.contains("CVE-") && !line.contains(", CVE-")) {
            line = line.replace("<td>", "").replaceAll("</td>", "").replaceAll("<br/>", "")
                .replaceAll("</p>", "").replaceAll("<td rowspan=\".\">", "").trim();
            System.out.println(line);
          }
          if (line.contains("http://") || line.contains("https://")) {
            ArrayList<String> patches = getLinksFromLine(line);
            for (String patch : patches) {
              if (patch.contains("platform/cts") || patch.contains("torvalds")
                  || patch.contains("kernel.org") || patch.contains("/kernel/")
                  || patch.contains("qcacld") || patch.contains("audio-kernel")
                  || patch.contains("patchwork") || patch.contains("lkml.org")
                  || patch.contains("qca-wifi-host-cmn") || patch.contains("redhat.com")
                  || patch.contains("twitter.com") || patch.contains("prima")
                  || patch.contains("spinics.net")) {
                continue; // TODO: Make this a toggle
              }
              patch = patch.replaceAll("%2F", "/");
              System.out.println("\tLink - " + getRepo(patch) + patch);
            }
          }
        }
      }
      page.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ArrayList<String> getLinksFromLine(String line) {
    ArrayList<String> out = new ArrayList<>();
    Pattern link = Pattern.compile("<a[^>]+href=[\\\"']?([\\\"'>]+)[\\\"']?[^>]*>(.+?)<\\/a>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL); //Credit: https://stackoverflow.com/a/5120599
    Matcher matcher = link.matcher(line);
    while (matcher.find()) {
      out.add(matcher.group().split("href=\"")[1].split("\"")[0]);
    }
    return out;
  }

  public static String getRepo(String line) {
    if (line.contains("android.googlesource.com/platform/")) {
      line = line.split(".com/platform/")[1].split("/\\+/")[0];
      return line + " - ";
    }
    if (line.contains("source.codeaurora.org/quic/la/platform") && !line.contains("kernel")) {
      line = line.split("la/platform/")[1].split("/commit/")[0];
      return line + " - ";
    }
    return "";
  }

}