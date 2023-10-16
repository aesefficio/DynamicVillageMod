package net.minecraft.network.protocol.game;

import com.google.common.collect.Queues;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundCommandsPacket implements Packet<ClientGamePacketListener> {
   private static final byte MASK_TYPE = 3;
   private static final byte FLAG_EXECUTABLE = 4;
   private static final byte FLAG_REDIRECT = 8;
   private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
   private static final byte TYPE_ROOT = 0;
   private static final byte TYPE_LITERAL = 1;
   private static final byte TYPE_ARGUMENT = 2;
   private final int rootIndex;
   private final List<ClientboundCommandsPacket.Entry> entries;

   public ClientboundCommandsPacket(RootCommandNode<SharedSuggestionProvider> pRoot) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> object2intmap = enumerateNodes(pRoot);
      this.entries = createEntries(object2intmap);
      this.rootIndex = object2intmap.getInt(pRoot);
   }

   public ClientboundCommandsPacket(FriendlyByteBuf pBuffer) {
      this.entries = pBuffer.readList(ClientboundCommandsPacket::readNode);
      this.rootIndex = pBuffer.readVarInt();
      validateEntries(this.entries);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeCollection(this.entries, (p_237642_, p_237643_) -> {
         p_237643_.write(p_237642_);
      });
      pBuffer.writeVarInt(this.rootIndex);
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> p_237631_, BiPredicate<ClientboundCommandsPacket.Entry, IntSet> p_237632_) {
      IntSet intset = new IntOpenHashSet(IntSets.fromTo(0, p_237631_.size()));

      while(!intset.isEmpty()) {
         boolean flag = intset.removeIf((p_237637_) -> {
            return p_237632_.test(p_237631_.get(p_237637_), intset);
         });
         if (!flag) {
            throw new IllegalStateException("Server sent an impossible command tree");
         }
      }

   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> pEntries) {
      validateEntries(pEntries, ClientboundCommandsPacket.Entry::canBuild);
      validateEntries(pEntries, ClientboundCommandsPacket.Entry::canResolve);
   }

   private static Object2IntMap<CommandNode<SharedSuggestionProvider>> enumerateNodes(RootCommandNode<SharedSuggestionProvider> pRootNode) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> object2intmap = new Object2IntOpenHashMap<>();
      Queue<CommandNode<SharedSuggestionProvider>> queue = Queues.newArrayDeque();
      queue.add(pRootNode);

      CommandNode<SharedSuggestionProvider> commandnode;
      while((commandnode = queue.poll()) != null) {
         if (!object2intmap.containsKey(commandnode)) {
            int i = object2intmap.size();
            object2intmap.put(commandnode, i);
            queue.addAll(commandnode.getChildren());
            if (commandnode.getRedirect() != null) {
               queue.add(commandnode.getRedirect());
            }
         }
      }

      return object2intmap;
   }

   private static List<ClientboundCommandsPacket.Entry> createEntries(Object2IntMap<CommandNode<SharedSuggestionProvider>> p_237627_) {
      ObjectArrayList<ClientboundCommandsPacket.Entry> objectarraylist = new ObjectArrayList<>(p_237627_.size());
      objectarraylist.size(p_237627_.size());

      for(Object2IntMap.Entry<CommandNode<SharedSuggestionProvider>> entry : Object2IntMaps.fastIterable(p_237627_)) {
         objectarraylist.set(entry.getIntValue(), createEntry(entry.getKey(), p_237627_));
      }

      return objectarraylist;
   }

   private static ClientboundCommandsPacket.Entry readNode(FriendlyByteBuf p_131888_) {
      byte b0 = p_131888_.readByte();
      int[] aint = p_131888_.readVarIntArray();
      int i = (b0 & 8) != 0 ? p_131888_.readVarInt() : 0;
      ClientboundCommandsPacket.NodeStub clientboundcommandspacket$nodestub = read(p_131888_, b0);
      return new ClientboundCommandsPacket.Entry(clientboundcommandspacket$nodestub, b0, i, aint);
   }

   @Nullable
   private static ClientboundCommandsPacket.NodeStub read(FriendlyByteBuf p_237639_, byte p_237640_) {
      int i = p_237640_ & 3;
      if (i == 2) {
         String s1 = p_237639_.readUtf();
         int j = p_237639_.readVarInt();
         ArgumentTypeInfo<?, ?> argumenttypeinfo = Registry.COMMAND_ARGUMENT_TYPE.byId(j);
         if (argumenttypeinfo == null) {
            return null;
         } else {
            ArgumentTypeInfo.Template<?> template = argumenttypeinfo.deserializeFromNetwork(p_237639_);
            ResourceLocation resourcelocation = (p_237640_ & 16) != 0 ? p_237639_.readResourceLocation() : null;
            return new ClientboundCommandsPacket.ArgumentNodeStub(s1, template, resourcelocation);
         }
      } else if (i == 1) {
         String s = p_237639_.readUtf();
         return new ClientboundCommandsPacket.LiteralNodeStub(s);
      } else {
         return null;
      }
   }

   private static ClientboundCommandsPacket.Entry createEntry(CommandNode<SharedSuggestionProvider> p_237622_, Object2IntMap<CommandNode<SharedSuggestionProvider>> p_237623_) {
      int i = 0;
      int j;
      if (p_237622_.getRedirect() != null) {
         i |= 8;
         j = p_237623_.getInt(p_237622_.getRedirect());
      } else {
         j = 0;
      }

      if (p_237622_.getCommand() != null) {
         i |= 4;
      }

      ClientboundCommandsPacket.NodeStub clientboundcommandspacket$nodestub;
      if (p_237622_ instanceof RootCommandNode) {
         i |= 0;
         clientboundcommandspacket$nodestub = null;
      } else if (p_237622_ instanceof ArgumentCommandNode) {
         ArgumentCommandNode<SharedSuggestionProvider, ?> argumentcommandnode = (ArgumentCommandNode)p_237622_;
         clientboundcommandspacket$nodestub = new ClientboundCommandsPacket.ArgumentNodeStub(argumentcommandnode);
         i |= 2;
         if (argumentcommandnode.getCustomSuggestions() != null) {
            i |= 16;
         }
      } else {
         if (!(p_237622_ instanceof LiteralCommandNode)) {
            throw new UnsupportedOperationException("Unknown node type " + p_237622_);
         }

         LiteralCommandNode literalcommandnode = (LiteralCommandNode)p_237622_;
         clientboundcommandspacket$nodestub = new ClientboundCommandsPacket.LiteralNodeStub(literalcommandnode.getLiteral());
         i |= 1;
      }

      int[] aint = p_237622_.getChildren().stream().mapToInt(p_237623_::getInt).toArray();
      return new ClientboundCommandsPacket.Entry(clientboundcommandspacket$nodestub, i, j, aint);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleCommands(this);
   }

   public RootCommandNode<SharedSuggestionProvider> getRoot(CommandBuildContext p_237625_) {
      return (RootCommandNode)(new ClientboundCommandsPacket.NodeResolver(p_237625_, this.entries)).resolve(this.rootIndex);
   }

   static class ArgumentNodeStub implements ClientboundCommandsPacket.NodeStub {
      private final String id;
      private final ArgumentTypeInfo.Template<?> argumentType;
      @Nullable
      private final ResourceLocation suggestionId;

      @Nullable
      private static ResourceLocation getSuggestionId(@Nullable SuggestionProvider<SharedSuggestionProvider> pProvider) {
         return pProvider != null ? SuggestionProviders.getName(pProvider) : null;
      }

      ArgumentNodeStub(String pId, ArgumentTypeInfo.Template<?> pArgumentType, @Nullable ResourceLocation pSuggestionId) {
         this.id = pId;
         this.argumentType = pArgumentType;
         this.suggestionId = pSuggestionId;
      }

      public ArgumentNodeStub(ArgumentCommandNode<SharedSuggestionProvider, ?> pArgumentNode) {
         this(pArgumentNode.getName(), ArgumentTypeInfos.unpack(pArgumentNode.getType()), getSuggestionId(pArgumentNode.getCustomSuggestions()));
      }

      public ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext pContext) {
         ArgumentType<?> argumenttype = this.argumentType.instantiate(pContext);
         RequiredArgumentBuilder<SharedSuggestionProvider, ?> requiredargumentbuilder = RequiredArgumentBuilder.argument(this.id, argumenttype);
         if (this.suggestionId != null) {
            requiredargumentbuilder.suggests(SuggestionProviders.getProvider(this.suggestionId));
         }

         return requiredargumentbuilder;
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeUtf(this.id);
         serializeCap(pBuffer, this.argumentType);
         if (this.suggestionId != null) {
            pBuffer.writeResourceLocation(this.suggestionId);
         }

      }

      private static <A extends ArgumentType<?>> void serializeCap(FriendlyByteBuf pBuffer, ArgumentTypeInfo.Template<A> pArgumentInfoTemplate) {
         serializeCap(pBuffer, pArgumentInfoTemplate.type(), pArgumentInfoTemplate);
      }

      private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(FriendlyByteBuf pBuffer, ArgumentTypeInfo<A, T> pArgumentInfo, ArgumentTypeInfo.Template<A> pArgumentInfoTemplate) {
         pBuffer.writeVarInt(Registry.COMMAND_ARGUMENT_TYPE.getId(pArgumentInfo));
         pArgumentInfo.serializeToNetwork((T)pArgumentInfoTemplate, pBuffer);
      }
   }

   static class Entry {
      @Nullable
      final ClientboundCommandsPacket.NodeStub stub;
      final int flags;
      final int redirect;
      final int[] children;

      Entry(@Nullable ClientboundCommandsPacket.NodeStub pStub, int pFlags, int pRedirect, int[] pChildren) {
         this.stub = pStub;
         this.flags = pFlags;
         this.redirect = pRedirect;
         this.children = pChildren;
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeByte(this.flags);
         pBuffer.writeVarIntArray(this.children);
         if ((this.flags & 8) != 0) {
            pBuffer.writeVarInt(this.redirect);
         }

         if (this.stub != null) {
            this.stub.write(pBuffer);
         }

      }

      public boolean canBuild(IntSet p_237673_) {
         if ((this.flags & 8) != 0) {
            return !p_237673_.contains(this.redirect);
         } else {
            return true;
         }
      }

      public boolean canResolve(IntSet p_237677_) {
         for(int i : this.children) {
            if (p_237677_.contains(i)) {
               return false;
            }
         }

         return true;
      }
   }

   static class LiteralNodeStub implements ClientboundCommandsPacket.NodeStub {
      private final String id;

      LiteralNodeStub(String p_237680_) {
         this.id = p_237680_;
      }

      public ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext p_237682_) {
         return LiteralArgumentBuilder.literal(this.id);
      }

      public void write(FriendlyByteBuf p_237684_) {
         p_237684_.writeUtf(this.id);
      }
   }

   static class NodeResolver {
      private final CommandBuildContext context;
      private final List<ClientboundCommandsPacket.Entry> entries;
      private final List<CommandNode<SharedSuggestionProvider>> nodes;

      NodeResolver(CommandBuildContext p_237689_, List<ClientboundCommandsPacket.Entry> p_237690_) {
         this.context = p_237689_;
         this.entries = p_237690_;
         ObjectArrayList<CommandNode<SharedSuggestionProvider>> objectarraylist = new ObjectArrayList<>();
         objectarraylist.size(p_237690_.size());
         this.nodes = objectarraylist;
      }

      public CommandNode<SharedSuggestionProvider> resolve(int p_237692_) {
         CommandNode<SharedSuggestionProvider> commandnode = this.nodes.get(p_237692_);
         if (commandnode != null) {
            return commandnode;
         } else {
            ClientboundCommandsPacket.Entry clientboundcommandspacket$entry = this.entries.get(p_237692_);
            CommandNode<SharedSuggestionProvider> commandnode1;
            if (clientboundcommandspacket$entry.stub == null) {
               commandnode1 = new RootCommandNode<>();
            } else {
               ArgumentBuilder<SharedSuggestionProvider, ?> argumentbuilder = clientboundcommandspacket$entry.stub.build(this.context);
               if ((clientboundcommandspacket$entry.flags & 8) != 0) {
                  argumentbuilder.redirect(this.resolve(clientboundcommandspacket$entry.redirect));
               }

               if ((clientboundcommandspacket$entry.flags & 4) != 0) {
                  argumentbuilder.executes((p_237694_) -> {
                     return 0;
                  });
               }

               commandnode1 = argumentbuilder.build();
            }

            this.nodes.set(p_237692_, commandnode1);

            for(int i : clientboundcommandspacket$entry.children) {
               CommandNode<SharedSuggestionProvider> commandnode2 = this.resolve(i);
               if (!(commandnode2 instanceof RootCommandNode)) {
                  commandnode1.addChild(commandnode2);
               }
            }

            return commandnode1;
         }
      }
   }

   interface NodeStub {
      ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext pContext);

      void write(FriendlyByteBuf pBuffer);
   }
}