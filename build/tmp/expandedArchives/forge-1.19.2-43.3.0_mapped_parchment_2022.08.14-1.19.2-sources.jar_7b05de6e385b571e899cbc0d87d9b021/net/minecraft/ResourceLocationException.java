package net.minecraft;

public class ResourceLocationException extends RuntimeException {
   public ResourceLocationException(String pMessage) {
      super(pMessage);
   }

   public ResourceLocationException(String pMessage, Throwable pCause) {
      super(pMessage, pCause);
   }
}