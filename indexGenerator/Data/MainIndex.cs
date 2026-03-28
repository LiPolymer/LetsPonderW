using System.Text.Json.Serialization;

namespace LetsPonderWIndexGenerator.Data;

public class MainIndex {
    public enum IndexItemType {
        Fragment,
        PackUrl
    }
    public class MainIndexItem {
        [JsonPropertyName("type")] 
        // ReSharper disable once MemberCanBePrivate.Global
        public string TypeString { get; set; } = string.Empty;
        
        [JsonPropertyName("author")]
        public required string Author { get; set; }
        
        [JsonIgnore]
        public IndexItemType Type {
            get {
                return TypeString switch {
                    "fragment" => IndexItemType.Fragment,
                    "packUrl" => IndexItemType.PackUrl,
                    _ => throw new ArgumentOutOfRangeException()
                };
            }
            set {
                TypeString = value switch {
                    IndexItemType.Fragment => "fragment",
                    IndexItemType.PackUrl => "packUrl",
                    _ => throw new ArgumentOutOfRangeException(nameof(value),value,null)
                };
            } 
        }
        
        [JsonPropertyName("pathway")]
        public required string Pathway { get; set; }
        
        [JsonPropertyName("hash")]
        public required string Hash { get; set; }
    }

    [JsonPropertyName("items")]
    public Dictionary<string,MainIndexItem> Items { get; set; } = [];
}