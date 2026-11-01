namespace Ludil.Core;
/// <summary>Invalidates queued results as soon as a capture is stopped or replaced.</summary>
public sealed class SessionGuard {
    long current;
    public long Begin()=>Interlocked.Increment(ref current);
    public void Invalidate()=>Interlocked.Increment(ref current);
    public bool IsCurrent(long ticket)=>Volatile.Read(ref current)==ticket;
}
