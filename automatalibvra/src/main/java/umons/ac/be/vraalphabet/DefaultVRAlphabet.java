package umons.ac.be.vraalphabet;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.ProceduralInputAlphabet;

/**
 * Default implementation of a {@link ProceduralInputAlphabet}.
 *
 * @param <I>
 *         input symbol type
 */
public class DefaultVRAlphabet<I> extends AbstractVRAlphabet<I> implements VRAlphabet<I> {
    public DefaultVRAlphabet(
            Alphabet<I> internalAlphabet,
            Alphabet<I> callAlphabet,
            Alphabet<I> returnAlphabet) {
        super(internalAlphabet, callAlphabet, returnAlphabet);
    }
}