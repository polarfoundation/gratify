package foundation.polar.gratify.annotations.utils;

import foundation.polar.gratify.annotations.Pure;
import foundation.polar.gratify.annotations.compilermsgs.CompilerMessageKey;
import foundation.polar.gratify.annotations.dataflow.SideEffectFree;
import foundation.polar.gratify.lang.Nullable;

import java.util.*;

public final class Result {
   private static enum Type {
      SUCCESS,
      FAILURE,
      WARNING;

      public static final Type merge(Type a, Type b) {
         if (a == FAILURE || b == FAILURE) {
            return FAILURE;
         } else if (a == WARNING || b == WARNING) {
            return WARNING;
         } else {
            return SUCCESS;
         }
      }
   }

   private final Type type;
   private final List<DiagMessage> messages;
   public static final Result SUCCESS = new Result(Type.SUCCESS, null);

   public static Result failure(@CompilerMessageKey String messageKey, @Nullable Object... args) {
      return new Result(Type.FAILURE, Collections.singleton(new DiagMessage(messageKey, args)));
   }

   public static Result warning(@CompilerMessageKey String messageKey, @Nullable Object... args) {
      return new Result(Type.WARNING, Collections.singleton(new DiagMessage(messageKey, args)));
   }

   private Result(Type type, Collection<DiagMessage> messagePairs) {
      this.type = type;
      this.messages = new ArrayList<>();
      if (messagePairs != null) {
         for (DiagMessage msg : messagePairs) {
            String message = msg.getMessageKey();
            @Nullable Object[] args = msg.getArgs();
            if (args != null) {
               args = Arrays.copyOf(msg.getArgs(), args.length);
            }
            this.messages.add(new DiagMessage(message, args));
         }
      }
   }

   public Result merge(Result r) {
      if (r == null) {
         return this;
      }

      if (r.isSuccess() && this.isSuccess()) {
         return SUCCESS;
      }

      List<DiagMessage> messages = new ArrayList<>(this.messages.size() + r.messages.size());
      messages.addAll(this.messages);
      messages.addAll(r.messages);
      return new Result(Type.merge(r.type, this.type), messages);
   }

   public boolean isSuccess() {
      return type == Type.SUCCESS;
   }

   public boolean isFailure() {
      return type == Type.FAILURE;
   }

   public boolean isWarning() {
      return type == Type.WARNING;
   }

   public List<String> getMessageKeys() {
      List<String> msgKeys = new ArrayList<>(getDiagMessages().size());
      for (DiagMessage msg : getDiagMessages()) {
         msgKeys.add(msg.getMessageKey());
      }

      return Collections.unmodifiableList(msgKeys);
   }

   public List<DiagMessage> getDiagMessages() {
      return Collections.unmodifiableList(messages);
   }

   @SideEffectFree
   @Override
   public String toString() {
      switch (type) {
         case FAILURE:
            return "FAILURE: " + messages;
         case WARNING:
            return "WARNING: " + messages;
         case SUCCESS:
         default:
            return "SUCCESS";
      }
   }

   public static class DiagMessage {
      private final @CompilerMessageKey String message;
      private Object[] args;

      protected DiagMessage(@CompilerMessageKey String message, Object[] args) {
         this.message = message;
         if (args == null) {
            this.args = new Object[0]; /*null->nn*/
         } else {
            this.args = Arrays.copyOf(args, args.length);
         }
      }

      public @CompilerMessageKey String getMessageKey() {
         return this.message;
      }

      public Object[] getArgs() {
         return this.args;
      }

      @Override
      public boolean equals(@Nullable Object obj) {
         if (!(obj instanceof DiagMessage)) {
            return false;
         }
         DiagMessage other = (DiagMessage) obj;
         return (message.equals(other.message) && Arrays.equals(args, other.args));
      }

      @Pure
      @Override
      public int hashCode() {
         return Objects.hash(this.message, Arrays.hashCode(this.args));
      }

      @SideEffectFree
      @Override
      public String toString() {
         if (args.length == 0) {
            return message;
         }
         return message + " : " + Arrays.toString(args);
      }
   }
}
