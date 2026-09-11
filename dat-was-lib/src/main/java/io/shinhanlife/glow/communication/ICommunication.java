package io.shinhanlife.glow.communication;

/** Minimal Glow communication contract used by the temporary compatibility layer. */
public interface ICommunication<I, O> {
    O sync(I request);
}