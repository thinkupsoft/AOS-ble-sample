package no.nordicsemi.android.meshprovisioner.transport;

/**
 * Abstract wrapper class for mesh message.
 */
public abstract class ProxyConfigMessage extends MeshMessage {

    /**
     * Creates the parameters for a given mesh message.
     */
    public abstract void assembleMessageParameters();

    /**
     * Returns the parameters of this message.
     *
     * @return parameters
     */
    public abstract byte[] getParameters();


    @Override
    public int getAkf() {
        return -1;
    }

    @Override
    public int getAid() {
        return -1;
    }

}
