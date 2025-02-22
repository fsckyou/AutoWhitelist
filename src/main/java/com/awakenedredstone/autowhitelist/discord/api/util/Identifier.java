package com.awakenedredstone.autowhitelist.discord.api.util;

import com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText;
import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * The namespace and path must contain only lowercase letters ([a-z]), digits ([0-9]), or the characters '_', '.', and '-'. The path can also contain the standard path separator '/'.
 */
public class Identifier implements Comparable<Identifier> {
   public static final Codec<Identifier> CODEC;
   private static final SimpleCommandExceptionType COMMAND_EXCEPTION;
   public static final char NAMESPACE_SEPARATOR = ':';
   public static final String DEFAULT_NAMESPACE = "minecraft";
   public static final String REALMS_NAMESPACE = "realms";
   protected final String namespace;
   protected final String path;

   protected Identifier(String[] id) {
      this.namespace = StringUtils.isEmpty(id[0]) ? "minecraft" : id[0];
      this.path = id[1];
      if (!isNamespaceValid(this.namespace)) {
         throw new InvalidIdentifierException("Non [a-z0-9_.-] character in namespace of location: " + this.namespace + ":" + this.path);
      } else if (!isPathValid(this.path)) {
         throw new InvalidIdentifierException("Non [a-z0-9/._-] character in path of location: " + this.namespace + ":" + this.path);
      }
   }

   /**
    * <p>Takes a string of the form {@code <namespace>:<path>}, for example {@code minecraft:iron_ingot}.
    * <p>The string will be split (on the {@code :}) into an identifier with the specified path and namespace.
    * Prefer using the {@link Identifier#Identifier(String, String) Identifier(java.lang.String, java.lang.String)} constructor that takes the namespace and path as individual parameters to avoid mistakes.
    * @throws InvalidIdentifierException if the string cannot be parsed as an identifier.
    */
   public Identifier(String id) {
      this(split(id, ':'));
   }

   public Identifier(String namespace, String path) {
      this(new String[]{namespace, path});
   }

   public static Identifier splitOn(String id, char delimiter) {
      return new Identifier(split(id, delimiter));
   }

   /**
    * <p>Parses a string into an {@code Identifier}.
    * Takes a string of the form {@code <namespace>:<path>}, for example {@code minecraft:iron_ingot}.
    * @return resulting identifier, or {@code null} if the string couldn't be parsed as an identifier
    */
   @Nullable
   public static Identifier tryParse(String id) {
      try {
         return new Identifier(id);
      } catch (InvalidIdentifierException var2) {
         return null;
      }
   }

   protected static String[] split(String id, char delimiter) {
      String[] strings = new String[]{"minecraft", id};
      int i = id.indexOf(delimiter);
      if (i >= 0) {
         strings[1] = id.substring(i + 1, id.length());
         if (i >= 1) {
            strings[0] = id.substring(0, i);
         }
      }

      return strings;
   }

   private static DataResult<Identifier> validate(String id) {
      try {
         return DataResult.success(new Identifier(id));
      } catch (InvalidIdentifierException var2) {
         return DataResult.error("Not a valid resource location: " + id + " " + var2.getMessage());
      }
   }

   public String getPath() {
      return this.path;
   }

   public String getNamespace() {
      return this.namespace;
   }

   public String toString() {
      return this.namespace + ":" + this.path;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof Identifier)) {
         return false;
      } else {
         Identifier identifier = (Identifier)o;
         return this.namespace.equals(identifier.namespace) && this.path.equals(identifier.path);
      }
   }

   public int hashCode() {
      return 31 * this.namespace.hashCode() + this.path.hashCode();
   }

   public int compareTo(Identifier identifier) {
      int i = this.path.compareTo(identifier.path);
      if (i == 0) {
         i = this.namespace.compareTo(identifier.namespace);
      }

      return i;
   }

   public String toUnderscoreSeparatedString() {
      return this.toString().replace('/', '_').replace(':', '_');
   }

   public static Identifier fromCommandInput(StringReader reader) throws CommandSyntaxException {
      int i = reader.getCursor();

      while(reader.canRead() && isCharValid(reader.peek())) {
         reader.skip();
      }

      String string = reader.getString().substring(i, reader.getCursor());

      try {
         return new Identifier(string);
      } catch (InvalidIdentifierException var4) {
         reader.setCursor(i);
         throw COMMAND_EXCEPTION.createWithContext(reader);
      }
   }

   public static boolean isCharValid(char c) {
      return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
   }

   private static boolean isPathValid(String path) {
      for(int i = 0; i < path.length(); ++i) {
         if (!isPathCharacterValid(path.charAt(i))) {
            return false;
         }
      }

      return true;
   }

   private static boolean isNamespaceValid(String namespace) {
      for(int i = 0; i < namespace.length(); ++i) {
         if (!isNamespaceCharacterValid(namespace.charAt(i))) {
            return false;
         }
      }

      return true;
   }

   public static boolean isPathCharacterValid(char character) {
      return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '/' || character == '.';
   }

   private static boolean isNamespaceCharacterValid(char character) {
      return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.';
   }

   public static boolean isValid(String id) {
      String[] strings = split(id, ':');
      return isNamespaceValid(StringUtils.isEmpty(strings[0]) ? "minecraft" : strings[0]) && isPathValid(strings[1]);
   }

   static {
      CODEC = Codec.STRING.comapFlatMap(Identifier::validate, Identifier::toString).stable();
      COMMAND_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("argument.id.invalid"));
   }

   public static class Serializer implements JsonDeserializer<Identifier>, JsonSerializer<Identifier> {
      public Identifier deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         return new Identifier(JsonHelper.asString(jsonElement, "location"));
      }

      public JsonElement serialize(Identifier identifier, Type type, JsonSerializationContext jsonSerializationContext) {
         return new JsonPrimitive(identifier.toString());
      }
   }
}
