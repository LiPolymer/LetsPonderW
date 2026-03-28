using System.Text.Encodings.Web;
using System.Text.Json;
using LetsPonderWIndexGenerator.Data;
using ShulkerRDK.Shared;

namespace LetsPonderWIndexGenerator;

static class Program {
    static void Main(string[] args) {
        Terminal.Init(new AnsiTerminal());
        Terminal.WriteLine("&3Let'sPonderW Index Generator");
        Terminal.WriteLine($"&8Parameters [{string.Join('|', args)}]");
        string rootP = Path.GetDirectoryName(Environment.ProcessPath!)!;
        string basePath = Path.Combine(rootP,"index/");
        if (!Directory.Exists(basePath)) Directory.CreateDirectory(basePath);
        if (args.Length != 0 && args[0].EndsWith(".zip")) ExtractExportedPondererPack(args[0]);
        MainIndex index = new MainIndex();
        foreach (string fd in Directory.GetDirectories(basePath)) {
            Terminal.WriteLine($"&8[&7FRA&8]&aIndexing&8[&7{Path.GetRelativePath(rootP,fd)}&8]");
            string metaP = Path.Combine(fd,"fragment.json");
            FragmentMeta meta = new FragmentMeta {
                Author = "Unknow"
            };
            if (File.Exists(metaP)) {
                meta = JsonSerializer.Deserialize<FragmentMeta>(File.ReadAllText(metaP))!;
            }
            meta.Files = [];
            string dataPath = Path.Combine(fd,"data/");
            if (!Directory.Exists(dataPath)) Directory.CreateDirectory(dataPath);
            foreach (string ff in Directory.GetFiles(Path.Combine(fd,"data/"),"*",SearchOption.AllDirectories)) {
                Terminal.WriteLine($"&8[FRA>&7FILE&8]&aAdding&8[&7{Path.GetRelativePath(rootP,ff)}&8]");
                meta.Files.Add(Path.GetRelativePath(Path.Combine(fd,"data/"),ff).Replace("\\","/"), Tools.GetSha1(ff));
            }
            File.WriteAllText(metaP,JsonSerializer.Serialize(meta,Jso));
            index.Items.Add(Path.GetFileName(fd),new MainIndex.MainIndexItem {
                Type = MainIndex.IndexItemType.Fragment,
                Pathway = Path.GetFileName(fd) + "/fragment.json",
                Hash = Tools.GetSha1(metaP),
                Author = meta.Author
            });
        }
        foreach (string im in Directory.GetFiles(basePath)) {
            if (Path.GetFileName(im) == "index.json") continue;
            string[] imi = Path.GetFileNameWithoutExtension(im).Split('.');
            switch (imi[1]) {
                case "url":
                    try {
                        Terminal.WriteLine($"&8[&7URL&8]&aIndexing&8[&7{Path.GetRelativePath(rootP,im)}&8]");
                        UrlMeta um = JsonSerializer.Deserialize<UrlMeta>(File.ReadAllText(im))!;
                        index.Items.Add(imi[0],new MainIndex.MainIndexItem {
                            Type = MainIndex.IndexItemType.PackUrl,
                            Pathway = um.Url,
                            Hash = um.Hash,
                            Author = um.Author
                        });
                    } catch {
                        File.WriteAllText(im,JsonSerializer.Serialize(new UrlMeta {
                            Author = "Unknown",
                            Url = "null",
                            Hash = "null"
                        }, Jso));
                    }
                    break;
                default:
                    continue;
            }
        }
        File.WriteAllText(Path.Combine(rootP,"index.json"),JsonSerializer.Serialize(index,Jso));
        Terminal.WriteLine($"&8[&7INDEX&8]&aFinished&8[&7{index.Items.Count} &8modId Loaded]");
        if (args.Length != 0) Console.ReadLine();
    }

    static void ExtractExportedPondererPack(string path) {
        
    }
    
    static readonly JsonSerializerOptions Jso = new JsonSerializerOptions {
        WriteIndented = true,
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };
}
