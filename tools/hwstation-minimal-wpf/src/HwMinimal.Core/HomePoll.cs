namespace HwMinimal;

/// <summary>
/// Homepage refresh after this panel's own HELLO PC succeeds.
/// Reads only. It does not connect by itself and does not send HELLO again.
/// </summary>
public static class HomePoll
{
    public const int IntervalMs = 1000;

    public static readonly string[] Verbs =
    {
        "READ_OVEN_TEMP",
        "READ_INLET_TEMP",
        "READ_DETECTOR_TEMP",
        "READ_GAS_PRESSURE",
        "READ_FID_STATUS",
        "READ_CONTROL_STATUS"
    };

    public static bool IsReadOnly
    {
        get
        {
            foreach (string verb in Verbs)
            {
                if (Gcws.Classify(Gcws.Line(verb)) != GcwsKind.Read || verb.Contains("HELLO", StringComparison.Ordinal))
                {
                    return false;
                }
            }
            return Verbs.Length == 6;
        }
    }

    public static bool? HeatFlag(string json)
    {
        if (!JsonFields.TryBool(json, "heat", out bool on))
        {
            return null;
        }
        return on;
    }
}

public sealed class HomePollGate
{
    public bool Active { get; private set; }

    public bool NoteHelloOk()
    {
        if (Active)
        {
            return false;
        }
        Active = true;
        return true;
    }

    public void NoteDisconnected() => Active = false;

    public bool MaySend(string verb)
    {
        if (!Active)
        {
            return false;
        }
        if (Gcws.Classify(Gcws.Line(verb)) != GcwsKind.Read)
        {
            return false;
        }
        foreach (string allowed in HomePoll.Verbs)
        {
            if (allowed == verb)
            {
                return true;
            }
        }
        return false;
    }
}
