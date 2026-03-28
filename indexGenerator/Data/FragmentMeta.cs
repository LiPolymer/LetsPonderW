using System.Text.Json.Serialization;

namespace LetsPonderWIndexGenerator.Data;

public class FragmentMeta {
    [JsonPropertyName("author")]
    public required string Author { get; set; }
    [JsonPropertyName("files")]
    public Dictionary<string,string> Files { get; set; } = [];
}