using System.Globalization;

namespace HwMinimal;

public sealed class PanelEvent
{
    public string Id { get; init; } = "";
    public string Name { get; init; } = "";
    public string Time { get; init; } = "";
    public string Detail { get; init; } = "";
}

public static class EventLog
{
    public static IReadOnlyList<PanelEvent> Load(string path)
    {
        if (!File.Exists(path))
        {
            return Array.Empty<PanelEvent>();
        }
        var list = new List<PanelEvent>();
        foreach (string line in File.ReadAllLines(path))
        {
            string[] part = line.Split('\t');
            if (part.Length < 4 || part[0].Contains("GCWS", StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }
            list.Add(new PanelEvent { Id = part[0], Time = part[1], Name = part[2], Detail = part[3] });
        }
        return list;
    }

    public static bool Report(string path, string id, string name, string detail)
    {
        List<PanelEvent> list = Load(path).ToList();
        if (list.Any(item => item.Id == id))
        {
            return false;
        }
        list.Insert(0, new PanelEvent
        {
            Id = id,
            Time = DateTime.Now.ToString("HH:mm:ss", CultureInfo.InvariantCulture),
            Name = name,
            Detail = detail
        });
        Save(path, list);
        return true;
    }

    public static void Clear(string path)
    {
        Save(path, Array.Empty<PanelEvent>());
    }

    private static void Save(string path, IReadOnlyList<PanelEvent> list)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(Path.GetFullPath(path))!);
        File.WriteAllLines(path, list.Select(item => item.Id + "\t" + item.Time + "\t" + item.Name + "\t" + item.Detail.Replace('\t', ' ').Replace('\n', ' ')));
    }
}
