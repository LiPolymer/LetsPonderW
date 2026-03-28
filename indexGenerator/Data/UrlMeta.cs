using System.Text.Json.Serialization;

namespace LetsPonderWIndexGenerator.Data;

public class UrlMeta {
    [JsonPropertyName("author")]
    public required string Author { get; set; }
    [JsonPropertyName("url")]
    public required string Url { get; set; }
    [JsonPropertyName("hash")] 
    public required string Hash { get; set; } 
}