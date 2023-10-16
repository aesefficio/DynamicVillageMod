package net.minecraft.client.player;

import com.google.common.collect.Lists;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundInstances;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ChatMessageContent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSigner;
import net.minecraft.network.chat.PreviewableCommand;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Signer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LocalPlayer extends AbstractClientPlayer {
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final int POSITION_REMINDER_INTERVAL = 20;
   private static final int WATER_VISION_MAX_TIME = 600;
   private static final int WATER_VISION_QUICK_TIME = 100;
   private static final float WATER_VISION_QUICK_PERCENT = 0.6F;
   private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35D;
   private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = (double)0.13962634F;
   private static final float DEFAULT_SNEAKING_MOVEMENT_FACTOR = 0.3F;
   public final ClientPacketListener connection;
   private final StatsCounter stats;
   private final ClientRecipeBook recipeBook;
   private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
   private int permissionLevel = 0;
   /**
    * The last X position which was transmitted to the server, used to determine when the X position changes and needs
    * to be re-trasmitted
    */
   private double xLast;
   /**
    * The last Y position which was transmitted to the server, used to determine when the Y position changes and needs
    * to be re-transmitted
    */
   private double yLast1;
   /**
    * The last Z position which was transmitted to the server, used to determine when the Z position changes and needs
    * to be re-transmitted
    */
   private double zLast;
   /**
    * The last yaw value which was transmitted to the server, used to determine when the yaw changes and needs to be re-
    * transmitted
    */
   private float yRotLast;
   /**
    * The last pitch value which was transmitted to the server, used to determine when the pitch changes and needs to be
    * re-transmitted
    */
   private float xRotLast;
   private boolean lastOnGround;
   private boolean crouching;
   private boolean wasShiftKeyDown;
   /** the last sprinting state sent to the server */
   private boolean wasSprinting;
   /**
    * Reset to 0 every time position is sent to the server, used to send periodic updates every 20 ticks even when the
    * player is not moving.
    */
   private int positionReminder;
   private boolean flashOnSetHealth;
   @Nullable
   private String serverBrand;
   public Input input;
   protected final Minecraft minecraft;
   protected int sprintTriggerTime;
   public int sprintTime;
   public float yBob;
   public float xBob;
   public float yBobO;
   public float xBobO;
   private int jumpRidingTicks;
   private float jumpRidingScale;
   public float portalTime;
   public float oPortalTime;
   private boolean startedUsingItem;
   @Nullable
   private InteractionHand usingItemHand;
   private boolean handsBusy;
   private boolean autoJumpEnabled = true;
   private int autoJumpTime;
   private boolean wasFallFlying;
   private int waterVisionTime;
   private boolean showDeathScreen = true;

   public LocalPlayer(Minecraft pMinecraft, ClientLevel pClientLevel, ClientPacketListener pConnection, StatsCounter pStats, ClientRecipeBook pRecipeBook, boolean pWasShiftKeyDown, boolean pWasSprinting) {
      super(pClientLevel, pConnection.getLocalGameProfile(), pMinecraft.getProfileKeyPairManager().profilePublicKey().orElse((ProfilePublicKey)null));
      this.minecraft = pMinecraft;
      this.connection = pConnection;
      this.stats = pStats;
      this.recipeBook = pRecipeBook;
      this.wasShiftKeyDown = pWasShiftKeyDown;
      this.wasSprinting = pWasSprinting;
      this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, pMinecraft.getSoundManager()));
      this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
      this.ambientSoundHandlers.add(new BiomeAmbientSoundsHandler(this, pMinecraft.getSoundManager(), pClientLevel.getBiomeManager()));
   }

   /**
    * Called when the entity is attacked.
    */
   public boolean hurt(DamageSource pSource, float pAmount) {
      net.minecraftforge.common.ForgeHooks.onPlayerAttack(this, pSource, pAmount);
      return false;
   }

   /**
    * Heal living entity (param: amount of half-hearts)
    */
   public void heal(float pHealAmount) {
   }

   public boolean startRiding(Entity pEntity, boolean pForce) {
      if (!super.startRiding(pEntity, pForce)) {
         return false;
      } else {
         if (pEntity instanceof AbstractMinecart) {
            this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)pEntity, true));
            this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)pEntity, false));
         }

         return true;
      }
   }

   public void removeVehicle() {
      super.removeVehicle();
      this.handsBusy = false;
   }

   /**
    * Returns the current X rotation of the entity.
    */
   public float getViewXRot(float pPartialTick) {
      return this.getXRot();
   }

   /**
    * Gets the current yaw of the entity
    */
   public float getViewYRot(float pPartialTick) {
      return this.isPassenger() ? super.getViewYRot(pPartialTick) : this.getYRot();
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      if (this.level.hasChunkAt(this.getBlockX(), this.getBlockZ())) {
         super.tick();
         if (this.isPassenger()) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround));
            this.connection.send(new ServerboundPlayerInputPacket(this.xxa, this.zza, this.input.jumping, this.input.shiftKeyDown));
            Entity entity = this.getRootVehicle();
            if (entity != this && entity.isControlledByLocalInstance()) {
               this.connection.send(new ServerboundMoveVehiclePacket(entity));
            }
         } else {
            this.sendPosition();
         }

         for(AmbientSoundHandler ambientsoundhandler : this.ambientSoundHandlers) {
            ambientsoundhandler.tick();
         }

      }
   }

   public float getCurrentMood() {
      for(AmbientSoundHandler ambientsoundhandler : this.ambientSoundHandlers) {
         if (ambientsoundhandler instanceof BiomeAmbientSoundsHandler) {
            return ((BiomeAmbientSoundsHandler)ambientsoundhandler).getMoodiness();
         }
      }

      return 0.0F;
   }

   /**
    * called every tick when the player is on foot. Performs all the things that normally happen during movement.
    */
   private void sendPosition() {
      boolean flag = this.isSprinting();
      if (flag != this.wasSprinting) {
         ServerboundPlayerCommandPacket.Action serverboundplayercommandpacket$action = flag ? ServerboundPlayerCommandPacket.Action.START_SPRINTING : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
         this.connection.send(new ServerboundPlayerCommandPacket(this, serverboundplayercommandpacket$action));
         this.wasSprinting = flag;
      }

      boolean flag3 = this.isShiftKeyDown();
      if (flag3 != this.wasShiftKeyDown) {
         ServerboundPlayerCommandPacket.Action serverboundplayercommandpacket$action1 = flag3 ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;
         this.connection.send(new ServerboundPlayerCommandPacket(this, serverboundplayercommandpacket$action1));
         this.wasShiftKeyDown = flag3;
      }

      if (this.isControlledCamera()) {
         double d4 = this.getX() - this.xLast;
         double d0 = this.getY() - this.yLast1;
         double d1 = this.getZ() - this.zLast;
         double d2 = (double)(this.getYRot() - this.yRotLast);
         double d3 = (double)(this.getXRot() - this.xRotLast);
         ++this.positionReminder;
         boolean flag1 = Mth.lengthSquared(d4, d0, d1) > Mth.square(2.0E-4D) || this.positionReminder >= 20;
         boolean flag2 = d2 != 0.0D || d3 != 0.0D;
         if (this.isPassenger()) {
            Vec3 vec3 = this.getDeltaMovement();
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(vec3.x, -999.0D, vec3.z, this.getYRot(), this.getXRot(), this.onGround));
            flag1 = false;
         } else if (flag1 && flag2) {
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround));
         } else if (flag1) {
            this.connection.send(new ServerboundMovePlayerPacket.Pos(this.getX(), this.getY(), this.getZ(), this.onGround));
         } else if (flag2) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround));
         } else if (this.lastOnGround != this.onGround) {
            this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(this.onGround));
         }

         if (flag1) {
            this.xLast = this.getX();
            this.yLast1 = this.getY();
            this.zLast = this.getZ();
            this.positionReminder = 0;
         }

         if (flag2) {
            this.yRotLast = this.getYRot();
            this.xRotLast = this.getXRot();
         }

         this.lastOnGround = this.onGround;
         this.autoJumpEnabled = this.minecraft.options.autoJump().get();
      }

   }

   public boolean drop(boolean pFullStack) {
      ServerboundPlayerActionPacket.Action serverboundplayeractionpacket$action = pFullStack ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : ServerboundPlayerActionPacket.Action.DROP_ITEM;
      ItemStack itemstack = this.getInventory().removeFromSelected(pFullStack);
      this.connection.send(new ServerboundPlayerActionPacket(serverboundplayeractionpacket$action, BlockPos.ZERO, Direction.DOWN));
      return !itemstack.isEmpty();
   }

   public void chatSigned(String pPlain, @Nullable Component pDecorated) {
      pPlain = net.minecraftforge.client.ForgeHooksClient.onClientSendMessage(pPlain);
      if (pPlain.isEmpty()) return;
      this.sendChat(pPlain, pDecorated);
   }

   public boolean commandHasSignableArguments(String pCommand) {
      ParseResults<SharedSuggestionProvider> parseresults = this.connection.getCommands().parse(pCommand, this.connection.getSuggestionsProvider());
      return ArgumentSignatures.hasSignableArguments(PreviewableCommand.of(parseresults));
   }

   public boolean commandUnsigned(String pCommand) {
      if (!this.commandHasSignableArguments(pCommand)) {
         LastSeenMessages.Update lastseenmessages$update = this.connection.generateMessageAcknowledgements();
         this.connection.send(new ServerboundChatCommandPacket(pCommand, Instant.now(), 0L, ArgumentSignatures.EMPTY, false, lastseenmessages$update));
         return true;
      } else {
         return false;
      }
   }

   public void commandSigned(String pPlain, @Nullable Component pDecorated) {
      this.sendCommand(pPlain, pDecorated);
   }

   private void sendChat(String pPlain, @Nullable Component pDecorated) {
      ChatMessageContent chatmessagecontent = this.buildSignedContent(pPlain, pDecorated);
      MessageSigner messagesigner = this.createMessageSigner();
      LastSeenMessages.Update lastseenmessages$update = this.connection.generateMessageAcknowledgements();
      MessageSignature messagesignature = this.signMessage(messagesigner, chatmessagecontent, lastseenmessages$update.lastSeen());
      this.connection.send(new ServerboundChatPacket(chatmessagecontent.plain(), messagesigner.timeStamp(), messagesigner.salt(), messagesignature, chatmessagecontent.isDecorated(), lastseenmessages$update));
   }

   private MessageSignature signMessage(MessageSigner pSigner, ChatMessageContent pMessageContent, LastSeenMessages pLastSeenMessages) {
      try {
         Signer signer = this.minecraft.getProfileKeyPairManager().signer();
         if (signer != null) {
            return this.connection.signedMessageEncoder().pack(signer, pSigner, pMessageContent, pLastSeenMessages).signature();
         }
      } catch (Exception exception) {
         LOGGER.error("Failed to sign chat message: '{}'", pMessageContent.plain(), exception);
      }

      return MessageSignature.EMPTY;
   }

   private void sendCommand(String pPlain, @Nullable Component pDecorated) {
      if (net.minecraftforge.client.ClientCommandHandler.runCommand(pPlain)) return;
      ParseResults<SharedSuggestionProvider> parseresults = this.connection.getCommands().parse(pPlain, this.connection.getSuggestionsProvider());
      MessageSigner messagesigner = this.createMessageSigner();
      LastSeenMessages.Update lastseenmessages$update = this.connection.generateMessageAcknowledgements();
      ArgumentSignatures argumentsignatures = this.signCommandArguments(messagesigner, parseresults, pDecorated, lastseenmessages$update.lastSeen());
      this.connection.send(new ServerboundChatCommandPacket(pPlain, messagesigner.timeStamp(), messagesigner.salt(), argumentsignatures, pDecorated != null, lastseenmessages$update));
   }

   private ArgumentSignatures signCommandArguments(MessageSigner pSigner, ParseResults<SharedSuggestionProvider> pParseResults, @Nullable Component p_242373_, LastSeenMessages p_242435_) {
      Signer signer = this.minecraft.getProfileKeyPairManager().signer();
      if (signer == null) {
         return ArgumentSignatures.EMPTY;
      } else {
         try {
            return ArgumentSignatures.signCommand(PreviewableCommand.of(pParseResults), (p_243155_, p_243156_) -> {
               ChatMessageContent chatmessagecontent = this.buildSignedContent(p_243156_, p_242373_);
               return this.connection.signedMessageEncoder().pack(signer, pSigner, chatmessagecontent, p_242435_).signature();
            });
         } catch (Exception exception) {
            LOGGER.error("Failed to sign command arguments", (Throwable)exception);
            return ArgumentSignatures.EMPTY;
         }
      }
   }

   private ChatMessageContent buildSignedContent(String pPlain, @Nullable Component pDecorated) {
      String s = StringUtil.trimChatMessage(pPlain);
      return pDecorated != null ? new ChatMessageContent(s, pDecorated) : new ChatMessageContent(s);
   }

   private MessageSigner createMessageSigner() {
      return MessageSigner.create(this.getUUID());
   }

   public void swing(InteractionHand pHand) {
      super.swing(pHand);
      this.connection.send(new ServerboundSwingPacket(pHand));
   }

   public void respawn() {
      this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
   }

   /**
    * Deals damage to the entity. This will take the armor of the entity into consideration before damaging the health
    * bar.
    */
   protected void actuallyHurt(DamageSource pDamageSrc, float pDamageAmount) {
      if (!this.isInvulnerableTo(pDamageSrc)) {
         this.setHealth(this.getHealth() - pDamageAmount);
      }
   }

   /**
    * set current crafting inventory back to the 2x2 square
    */
   public void closeContainer() {
      this.connection.send(new ServerboundContainerClosePacket(this.containerMenu.containerId));
      this.clientSideCloseContainer();
   }

   public void clientSideCloseContainer() {
      super.closeContainer();
      this.minecraft.setScreen((Screen)null);
   }

   /**
    * Updates health locally.
    */
   public void hurtTo(float pHealth) {
      if (this.flashOnSetHealth) {
         float f = this.getHealth() - pHealth;
         if (f <= 0.0F) {
            this.setHealth(pHealth);
            if (f < 0.0F) {
               this.invulnerableTime = 10;
            }
         } else {
            this.lastHurt = f;
            this.invulnerableTime = 20;
            this.setHealth(pHealth);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }
      } else {
         this.setHealth(pHealth);
         this.flashOnSetHealth = true;
      }

   }

   /**
    * Sends the player's abilities to the server (if there is one).
    */
   public void onUpdateAbilities() {
      this.connection.send(new ServerboundPlayerAbilitiesPacket(this.getAbilities()));
   }

   /**
    * returns true if this is an EntityPlayerSP, or the logged in player.
    */
   public boolean isLocalPlayer() {
      return true;
   }

   public boolean isSuppressingSlidingDownLadder() {
      return !this.getAbilities().flying && super.isSuppressingSlidingDownLadder();
   }

   public boolean canSpawnSprintParticle() {
      return !this.getAbilities().flying && super.canSpawnSprintParticle();
   }

   public boolean canSpawnSoulSpeedParticle() {
      return !this.getAbilities().flying && super.canSpawnSoulSpeedParticle();
   }

   protected void sendRidingJump() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, Mth.floor(this.getJumpRidingScale() * 100.0F)));
   }

   public void sendOpenInventory() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
   }

   /**
    * Sets the brand of the currently connected server. Server brand information is sent over the {@code MC|Brand}
    * plugin channel, and is used to identify modded servers in crash reports.
    */
   public void setServerBrand(@Nullable String pServerBrand) {
      this.serverBrand = pServerBrand;
   }

   /**
    * Gets the brand of the currently connected server. May be null if the server hasn't yet sent brand information.
    * Server brand information is sent over the {@code MC|Brand} plugin channel, and is used to identify modded servers
    * in crash reports.
    */
   @Nullable
   public String getServerBrand() {
      return this.serverBrand;
   }

   public StatsCounter getStats() {
      return this.stats;
   }

   public ClientRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   public void removeRecipeHighlight(Recipe<?> pRecipe) {
      if (this.recipeBook.willHighlight(pRecipe)) {
         this.recipeBook.removeHighlight(pRecipe);
         this.connection.send(new ServerboundRecipeBookSeenRecipePacket(pRecipe));
      }

   }

   public int getPermissionLevel() {
      return this.permissionLevel;
   }

   public void setPermissionLevel(int pPermissionLevel) {
      this.permissionLevel = pPermissionLevel;
   }

   public void displayClientMessage(Component pChatComponent, boolean pActionBar) {
      this.minecraft.getChatListener().handleSystemMessage(pChatComponent, pActionBar);
   }

   private void moveTowardsClosestSpace(double pX, double pZ) {
      BlockPos blockpos = new BlockPos(pX, this.getY(), pZ);
      if (this.suffocatesAt(blockpos)) {
         double d0 = pX - (double)blockpos.getX();
         double d1 = pZ - (double)blockpos.getZ();
         Direction direction = null;
         double d2 = Double.MAX_VALUE;
         Direction[] adirection = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

         for(Direction direction1 : adirection) {
            double d3 = direction1.getAxis().choose(d0, 0.0D, d1);
            double d4 = direction1.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0D - d3 : d3;
            if (d4 < d2 && !this.suffocatesAt(blockpos.relative(direction1))) {
               d2 = d4;
               direction = direction1;
            }
         }

         if (direction != null) {
            Vec3 vec3 = this.getDeltaMovement();
            if (direction.getAxis() == Direction.Axis.X) {
               this.setDeltaMovement(0.1D * (double)direction.getStepX(), vec3.y, vec3.z);
            } else {
               this.setDeltaMovement(vec3.x, vec3.y, 0.1D * (double)direction.getStepZ());
            }
         }

      }
   }

   private boolean suffocatesAt(BlockPos pPos) {
      AABB aabb = this.getBoundingBox();
      AABB aabb1 = (new AABB((double)pPos.getX(), aabb.minY, (double)pPos.getZ(), (double)pPos.getX() + 1.0D, aabb.maxY, (double)pPos.getZ() + 1.0D)).deflate(1.0E-7D);
      return this.level.collidesWithSuffocatingBlock(this, aabb1);
   }

   /**
    * Set sprinting switch for Entity.
    */
   public void setSprinting(boolean pSprinting) {
      super.setSprinting(pSprinting);
      this.sprintTime = 0;
   }

   /**
    * Sets the current XP, total XP, and level number.
    */
   public void setExperienceValues(float pCurrentXP, int pMaxXP, int pLevel) {
      this.experienceProgress = pCurrentXP;
      this.totalExperience = pMaxXP;
      this.experienceLevel = pLevel;
   }

   public void sendSystemMessage(Component pComponent) {
      this.minecraft.gui.getChat().addMessage(pComponent);
   }

   /**
    * Handles an entity event fired from {@link net.minecraft.world.level.Level#broadcastEntityEvent}.
    */
   public void handleEntityEvent(byte pId) {
      if (pId >= 24 && pId <= 28) {
         this.setPermissionLevel(pId - 24);
      } else {
         super.handleEntityEvent(pId);
      }

   }

   public void setShowDeathScreen(boolean pShow) {
      this.showDeathScreen = pShow;
   }

   public boolean shouldShowDeathScreen() {
      return this.showDeathScreen;
   }

   public void playSound(SoundEvent pSound, float pVolume, float pPitch) {
      net.minecraftforge.event.PlayLevelSoundEvent.AtEntity event = net.minecraftforge.event.ForgeEventFactory.onPlaySoundAtEntity(this, pSound, this.getSoundSource(), pVolume, pPitch);
      if (event.isCanceled() || event.getSound() == null) return;
      pSound = event.getSound();
      SoundSource source = event.getSource();
      pVolume = event.getNewVolume();
      pPitch = event.getNewPitch();
      this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), pSound, source, pVolume, pPitch, false);
   }

   public void playNotifySound(SoundEvent pSound, SoundSource pSource, float pVolume, float pPitch) {
      this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), pSound, pSource, pVolume, pPitch, false);
   }

   /**
    * Returns whether the entity is in a server world
    */
   public boolean isEffectiveAi() {
      return true;
   }

   public void startUsingItem(InteractionHand pHand) {
      ItemStack itemstack = this.getItemInHand(pHand);
      if (!itemstack.isEmpty() && !this.isUsingItem()) {
         super.startUsingItem(pHand);
         this.startedUsingItem = true;
         this.usingItemHand = pHand;
      }
   }

   public boolean isUsingItem() {
      return this.startedUsingItem;
   }

   public void stopUsingItem() {
      super.stopUsingItem();
      this.startedUsingItem = false;
   }

   public InteractionHand getUsedItemHand() {
      return Objects.requireNonNullElse(this.usingItemHand, InteractionHand.MAIN_HAND);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      super.onSyncedDataUpdated(pKey);
      if (DATA_LIVING_ENTITY_FLAGS.equals(pKey)) {
         boolean flag = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
         InteractionHand interactionhand = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         if (flag && !this.startedUsingItem) {
            this.startUsingItem(interactionhand);
         } else if (!flag && this.startedUsingItem) {
            this.stopUsingItem();
         }
      }

      if (DATA_SHARED_FLAGS_ID.equals(pKey) && this.isFallFlying() && !this.wasFallFlying) {
         this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
      }

   }

   public boolean isRidingJumpable() {
      Entity entity = this.getVehicle();
      return this.isPassenger() && entity instanceof PlayerRideableJumping && ((PlayerRideableJumping)entity).canJump();
   }

   public float getJumpRidingScale() {
      return this.jumpRidingScale;
   }

   public void openTextEdit(SignBlockEntity pSignTile) {
      this.minecraft.setScreen(new SignEditScreen(pSignTile, this.minecraft.isTextFilteringEnabled()));
   }

   public void openMinecartCommandBlock(BaseCommandBlock pCommandBlock) {
      this.minecraft.setScreen(new MinecartCommandBlockEditScreen(pCommandBlock));
   }

   public void openCommandBlock(CommandBlockEntity pCommandBlock) {
      this.minecraft.setScreen(new CommandBlockEditScreen(pCommandBlock));
   }

   public void openStructureBlock(StructureBlockEntity pStructure) {
      this.minecraft.setScreen(new StructureBlockEditScreen(pStructure));
   }

   public void openJigsawBlock(JigsawBlockEntity pJigsawBlockEntity) {
      this.minecraft.setScreen(new JigsawBlockEditScreen(pJigsawBlockEntity));
   }

   public void openItemGui(ItemStack pStack, InteractionHand pHand) {
      if (pStack.is(Items.WRITABLE_BOOK)) {
         this.minecraft.setScreen(new BookEditScreen(this, pStack, pHand));
      }

   }

   /**
    * Called when the entity is dealt a critical hit.
    */
   public void crit(Entity pEntityHit) {
      this.minecraft.particleEngine.createTrackingEmitter(pEntityHit, ParticleTypes.CRIT);
   }

   public void magicCrit(Entity pEntityHit) {
      this.minecraft.particleEngine.createTrackingEmitter(pEntityHit, ParticleTypes.ENCHANTED_HIT);
   }

   public boolean isShiftKeyDown() {
      return this.input != null && this.input.shiftKeyDown;
   }

   public boolean isCrouching() {
      return this.crouching;
   }

   public boolean isMovingSlowly() {
      return this.isCrouching() || this.isVisuallyCrawling();
   }

   public void serverAiStep() {
      super.serverAiStep();
      if (this.isControlledCamera()) {
         this.xxa = this.input.leftImpulse;
         this.zza = this.input.forwardImpulse;
         this.jumping = this.input.jumping;
         this.yBobO = this.yBob;
         this.xBobO = this.xBob;
         this.xBob += (this.getXRot() - this.xBob) * 0.5F;
         this.yBob += (this.getYRot() - this.yBob) * 0.5F;
      }

   }

   protected boolean isControlledCamera() {
      return this.minecraft.getCameraEntity() == this;
   }

   public void resetPos() {
      this.setPose(Pose.STANDING);
      if (this.level != null) {
         for(double d0 = this.getY(); d0 > (double)this.level.getMinBuildHeight() && d0 < (double)this.level.getMaxBuildHeight(); ++d0) {
            this.setPos(this.getX(), d0, this.getZ());
            if (this.level.noCollision(this)) {
               break;
            }
         }

         this.setDeltaMovement(Vec3.ZERO);
         this.setXRot(0.0F);
      }

      this.setHealth(this.getMaxHealth());
      this.deathTime = 0;
   }

   /**
    * Called every tick so the entity can update its state as required. For example, zombies and skeletons use this to
    * react to sunlight and start to burn.
    */
   public void aiStep() {
      ++this.sprintTime;
      if (this.sprintTriggerTime > 0) {
         --this.sprintTriggerTime;
      }

      this.handleNetherPortalClient();
      boolean flag = this.input.jumping;
      boolean flag1 = this.input.shiftKeyDown;
      boolean flag2 = this.hasEnoughImpulseToStartSprinting();
      this.crouching = !this.getAbilities().flying && !this.isSwimming() && this.canEnterPose(Pose.CROUCHING) && (this.isShiftKeyDown() || !this.isSleeping() && !this.canEnterPose(Pose.STANDING));
      float f = Mth.clamp(0.3F + EnchantmentHelper.getSneakingSpeedBonus(this), 0.0F, 1.0F);
      this.input.tick(this.isMovingSlowly(), f);
      net.minecraftforge.client.ForgeHooksClient.onMovementInputUpdate(this, this.input);
      this.minecraft.getTutorial().onInput(this.input);
      if (this.isUsingItem() && !this.isPassenger()) {
         this.input.leftImpulse *= 0.2F;
         this.input.forwardImpulse *= 0.2F;
         this.sprintTriggerTime = 0;
      }

      boolean flag3 = false;
      if (this.autoJumpTime > 0) {
         --this.autoJumpTime;
         flag3 = true;
         this.input.jumping = true;
      }

      if (!this.noPhysics) {
         this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35D, this.getZ() + (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35D, this.getZ() - (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35D, this.getZ() - (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35D, this.getZ() + (double)this.getBbWidth() * 0.35D);
      }

      if (flag1) {
         this.sprintTriggerTime = 0;
      }

      boolean flag4 = (float)this.getFoodData().getFoodLevel() > 6.0F || this.getAbilities().mayfly;
      if ((this.onGround || this.isUnderWater() || this.canStartSwimming()) && !flag1 && !flag2 && this.hasEnoughImpulseToStartSprinting() && !this.isSprinting() && flag4 && !this.isUsingItem() && !this.hasEffect(MobEffects.BLINDNESS)) {
         if (this.sprintTriggerTime <= 0 && !this.minecraft.options.keySprint.isDown()) {
            this.sprintTriggerTime = 7;
         } else {
            this.setSprinting(true);
         }
      }

      if (!this.isSprinting() && (!(this.isInWater() || this.isInFluidType((fluidType, height) -> this.canSwimInFluidType(fluidType))) || (this.isUnderWater() || this.canStartSwimming())) && this.hasEnoughImpulseToStartSprinting() && flag4 && !this.isUsingItem() && !this.hasEffect(MobEffects.BLINDNESS) && this.minecraft.options.keySprint.isDown()) {
         this.setSprinting(true);
      }

      if (this.isSprinting()) {
         boolean flag5 = !this.input.hasForwardImpulse() || !flag4;
         boolean flag6 = flag5 || this.horizontalCollision && !this.minorHorizontalCollision || (this.isInWater() && !this.isUnderWater()) || (this.isInFluidType((fluidType, height) -> this.canSwimInFluidType(fluidType)) && !this.canStartSwimming());
         if (this.isSwimming()) {
            if (!this.onGround && !this.input.shiftKeyDown && flag5 || !(this.isInWater() || this.isInFluidType((fluidType, height) -> this.canSwimInFluidType(fluidType)))) {
               this.setSprinting(false);
            }
         } else if (flag6) {
            this.setSprinting(false);
         }
      }

      boolean flag7 = false;
      if (this.getAbilities().mayfly) {
         if (this.minecraft.gameMode.isAlwaysFlying()) {
            if (!this.getAbilities().flying) {
               this.getAbilities().flying = true;
               flag7 = true;
               this.onUpdateAbilities();
            }
         } else if (!flag && this.input.jumping && !flag3) {
            if (this.jumpTriggerTime == 0) {
               this.jumpTriggerTime = 7;
            } else if (!this.isSwimming()) {
               this.getAbilities().flying = !this.getAbilities().flying;
               flag7 = true;
               this.onUpdateAbilities();
               this.jumpTriggerTime = 0;
            }
         }
      }

      if (this.input.jumping && !flag7 && !flag && !this.getAbilities().flying && !this.isPassenger() && !this.onClimbable()) {
         ItemStack itemstack = this.getItemBySlot(EquipmentSlot.CHEST);
         if (itemstack.canElytraFly(this) && this.tryToStartFallFlying()) {
            this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
         }
      }

      this.wasFallFlying = this.isFallFlying();
      net.minecraftforge.fluids.FluidType fluidType = this.getMaxHeightFluidType();
      if ((this.isInWater() || (!fluidType.isAir() && this.canSwimInFluidType(fluidType))) && this.input.shiftKeyDown && this.isAffectedByFluids()) {
         this.sinkInFluid(this.isInWater() ? net.minecraftforge.common.ForgeMod.WATER_TYPE.get() : fluidType);
      }

      if (this.isEyeInFluid(FluidTags.WATER)) {
         int i = this.isSpectator() ? 10 : 1;
         this.waterVisionTime = Mth.clamp(this.waterVisionTime + i, 0, 600);
      } else if (this.waterVisionTime > 0) {
         this.isEyeInFluid(FluidTags.WATER);
         this.waterVisionTime = Mth.clamp(this.waterVisionTime - 10, 0, 600);
      }

      if (this.getAbilities().flying && this.isControlledCamera()) {
         int j = 0;
         if (this.input.shiftKeyDown) {
            --j;
         }

         if (this.input.jumping) {
            ++j;
         }

         if (j != 0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)((float)j * this.getAbilities().getFlyingSpeed() * 3.0F), 0.0D));
         }
      }

      if (this.isRidingJumpable()) {
         PlayerRideableJumping playerrideablejumping = (PlayerRideableJumping)this.getVehicle();
         if (this.jumpRidingTicks < 0) {
            ++this.jumpRidingTicks;
            if (this.jumpRidingTicks == 0) {
               this.jumpRidingScale = 0.0F;
            }
         }

         if (flag && !this.input.jumping) {
            this.jumpRidingTicks = -10;
            playerrideablejumping.onPlayerJump(Mth.floor(this.getJumpRidingScale() * 100.0F));
            this.sendRidingJump();
         } else if (!flag && this.input.jumping) {
            this.jumpRidingTicks = 0;
            this.jumpRidingScale = 0.0F;
         } else if (flag) {
            ++this.jumpRidingTicks;
            if (this.jumpRidingTicks < 10) {
               this.jumpRidingScale = (float)this.jumpRidingTicks * 0.1F;
            } else {
               this.jumpRidingScale = 0.8F + 2.0F / (float)(this.jumpRidingTicks - 9) * 0.1F;
            }
         }
      } else {
         this.jumpRidingScale = 0.0F;
      }

      super.aiStep();
      if (this.onGround && this.getAbilities().flying && !this.minecraft.gameMode.isAlwaysFlying()) {
         this.getAbilities().flying = false;
         this.onUpdateAbilities();
      }

   }

   /**
    * handles entity death timer, experience orb and particle creation
    */
   protected void tickDeath() {
      ++this.deathTime;
      if (this.deathTime == 20) {
         this.remove(Entity.RemovalReason.KILLED);
      }

   }

   private void handleNetherPortalClient() {
      this.oPortalTime = this.portalTime;
      if (this.isInsidePortal) {
         if (this.minecraft.screen != null && !this.minecraft.screen.isPauseScreen() && !(this.minecraft.screen instanceof DeathScreen) && !(this.minecraft.screen instanceof ReceivingLevelScreen)) {
            if (this.minecraft.screen instanceof AbstractContainerScreen) {
               this.closeContainer();
            }

            this.minecraft.setScreen((Screen)null);
         }

         if (this.portalTime == 0.0F) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4F + 0.8F, 0.25F));
         }

         this.portalTime += 0.0125F;
         if (this.portalTime >= 1.0F) {
            this.portalTime = 1.0F;
         }

         this.isInsidePortal = false;
      } else if (this.hasEffect(MobEffects.CONFUSION) && this.getEffect(MobEffects.CONFUSION).getDuration() > 60) {
         this.portalTime += 0.006666667F;
         if (this.portalTime > 1.0F) {
            this.portalTime = 1.0F;
         }
      } else {
         if (this.portalTime > 0.0F) {
            this.portalTime -= 0.05F;
         }

         if (this.portalTime < 0.0F) {
            this.portalTime = 0.0F;
         }
      }

      this.processPortalCooldown();
   }

   /**
    * Handles updating while riding another entity
    */
   public void rideTick() {
      super.rideTick();
      if (this.wantsToStopRiding() && this.isPassenger()) this.input.shiftKeyDown = false;
      this.handsBusy = false;
      if (this.getVehicle() instanceof Boat) {
         Boat boat = (Boat)this.getVehicle();
         boat.setInput(this.input.left, this.input.right, this.input.up, this.input.down);
         this.handsBusy |= this.input.left || this.input.right || this.input.up || this.input.down;
      }

   }

   public boolean isHandsBusy() {
      return this.handsBusy;
   }

   /**
    * Removes the given potion effect from the active potion map and returns it. Does not call cleanup callbacks for the
    * end of the potion effect.
    */
   @Nullable
   public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect pPotioneffectin) {
      if (pPotioneffectin == MobEffects.CONFUSION) {
         this.oPortalTime = 0.0F;
         this.portalTime = 0.0F;
      }

      return super.removeEffectNoUpdate(pPotioneffectin);
   }

   public void move(MoverType pType, Vec3 pPos) {
      double d0 = this.getX();
      double d1 = this.getZ();
      super.move(pType, pPos);
      this.updateAutoJump((float)(this.getX() - d0), (float)(this.getZ() - d1));
   }

   public boolean isAutoJumpEnabled() {
      return this.autoJumpEnabled;
   }

   protected void updateAutoJump(float pMovementX, float pMovementZ) {
      if (this.canAutoJump()) {
         Vec3 vec3 = this.position();
         Vec3 vec31 = vec3.add((double)pMovementX, 0.0D, (double)pMovementZ);
         Vec3 vec32 = new Vec3((double)pMovementX, 0.0D, (double)pMovementZ);
         float f = this.getSpeed();
         float f1 = (float)vec32.lengthSqr();
         if (f1 <= 0.001F) {
            Vec2 vec2 = this.input.getMoveVector();
            float f2 = f * vec2.x;
            float f3 = f * vec2.y;
            float f4 = Mth.sin(this.getYRot() * ((float)Math.PI / 180F));
            float f5 = Mth.cos(this.getYRot() * ((float)Math.PI / 180F));
            vec32 = new Vec3((double)(f2 * f5 - f3 * f4), vec32.y, (double)(f3 * f5 + f2 * f4));
            f1 = (float)vec32.lengthSqr();
            if (f1 <= 0.001F) {
               return;
            }
         }

         float f12 = Mth.fastInvSqrt(f1);
         Vec3 vec312 = vec32.scale((double)f12);
         Vec3 vec313 = this.getForward();
         float f13 = (float)(vec313.x * vec312.x + vec313.z * vec312.z);
         if (!(f13 < -0.15F)) {
            CollisionContext collisioncontext = CollisionContext.of(this);
            BlockPos blockpos = new BlockPos(this.getX(), this.getBoundingBox().maxY, this.getZ());
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (blockstate.getCollisionShape(this.level, blockpos, collisioncontext).isEmpty()) {
               blockpos = blockpos.above();
               BlockState blockstate1 = this.level.getBlockState(blockpos);
               if (blockstate1.getCollisionShape(this.level, blockpos, collisioncontext).isEmpty()) {
                  float f6 = 7.0F;
                  float f7 = 1.2F;
                  if (this.hasEffect(MobEffects.JUMP)) {
                     f7 += (float)(this.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.75F;
                  }

                  float f8 = Math.max(f * 7.0F, 1.0F / f12);
                  Vec3 vec34 = vec31.add(vec312.scale((double)f8));
                  float f9 = this.getBbWidth();
                  float f10 = this.getBbHeight();
                  AABB aabb = (new AABB(vec3, vec34.add(0.0D, (double)f10, 0.0D))).inflate((double)f9, 0.0D, (double)f9);
                  Vec3 $$23 = vec3.add(0.0D, (double)0.51F, 0.0D);
                  vec34 = vec34.add(0.0D, (double)0.51F, 0.0D);
                  Vec3 vec35 = vec312.cross(new Vec3(0.0D, 1.0D, 0.0D));
                  Vec3 vec36 = vec35.scale((double)(f9 * 0.5F));
                  Vec3 vec37 = $$23.subtract(vec36);
                  Vec3 vec38 = vec34.subtract(vec36);
                  Vec3 vec39 = $$23.add(vec36);
                  Vec3 vec310 = vec34.add(vec36);
                  Iterable<VoxelShape> iterable = this.level.getCollisions(this, aabb);
                  Iterator<AABB> iterator = StreamSupport.stream(iterable.spliterator(), false).flatMap((p_234124_) -> {
                     return p_234124_.toAabbs().stream();
                  }).iterator();
                  float f11 = Float.MIN_VALUE;

                  while(iterator.hasNext()) {
                     AABB aabb1 = iterator.next();
                     if (aabb1.intersects(vec37, vec38) || aabb1.intersects(vec39, vec310)) {
                        f11 = (float)aabb1.maxY;
                        Vec3 vec311 = aabb1.getCenter();
                        BlockPos blockpos1 = new BlockPos(vec311);

                        for(int i = 1; (float)i < f7; ++i) {
                           BlockPos blockpos2 = blockpos1.above(i);
                           BlockState blockstate2 = this.level.getBlockState(blockpos2);
                           VoxelShape voxelshape;
                           if (!(voxelshape = blockstate2.getCollisionShape(this.level, blockpos2, collisioncontext)).isEmpty()) {
                              f11 = (float)voxelshape.max(Direction.Axis.Y) + (float)blockpos2.getY();
                              if ((double)f11 - this.getY() > (double)f7) {
                                 return;
                              }
                           }

                           if (i > 1) {
                              blockpos = blockpos.above();
                              BlockState blockstate3 = this.level.getBlockState(blockpos);
                              if (!blockstate3.getCollisionShape(this.level, blockpos, collisioncontext).isEmpty()) {
                                 return;
                              }
                           }
                        }
                        break;
                     }
                  }

                  if (f11 != Float.MIN_VALUE) {
                     float f14 = (float)((double)f11 - this.getY());
                     if (!(f14 <= 0.5F) && !(f14 > f7)) {
                        this.autoJumpTime = 1;
                     }
                  }
               }
            }
         }
      }
   }

   protected boolean isHorizontalCollisionMinor(Vec3 pDeltaMovement) {
      float f = this.getYRot() * ((float)Math.PI / 180F);
      double d0 = (double)Mth.sin(f);
      double d1 = (double)Mth.cos(f);
      double d2 = (double)this.xxa * d1 - (double)this.zza * d0;
      double d3 = (double)this.zza * d1 + (double)this.xxa * d0;
      double d4 = Mth.square(d2) + Mth.square(d3);
      double d5 = Mth.square(pDeltaMovement.x) + Mth.square(pDeltaMovement.z);
      if (!(d4 < (double)1.0E-5F) && !(d5 < (double)1.0E-5F)) {
         double d6 = d2 * pDeltaMovement.x + d3 * pDeltaMovement.z;
         double d7 = Math.acos(d6 / Math.sqrt(d4 * d5));
         return d7 < (double)0.13962634F;
      } else {
         return false;
      }
   }

   private boolean canAutoJump() {
      return this.isAutoJumpEnabled() && this.autoJumpTime <= 0 && this.onGround && !this.isStayingOnGroundSurface() && !this.isPassenger() && this.isMoving() && (double)this.getBlockJumpFactor() >= 1.0D;
   }

   private boolean isMoving() {
      Vec2 vec2 = this.input.getMoveVector();
      return vec2.x != 0.0F || vec2.y != 0.0F;
   }

   private boolean hasEnoughImpulseToStartSprinting() {
      double d0 = 0.8D;
      return this.isUnderWater() ? this.input.hasForwardImpulse() : (double)this.input.forwardImpulse >= 0.8D;
   }

   public float getWaterVision() {
      if (!this.isEyeInFluid(FluidTags.WATER)) {
         return 0.0F;
      } else {
         float f = 600.0F;
         float f1 = 100.0F;
         if ((float)this.waterVisionTime >= 600.0F) {
            return 1.0F;
         } else {
            float f2 = Mth.clamp((float)this.waterVisionTime / 100.0F, 0.0F, 1.0F);
            float f3 = (float)this.waterVisionTime < 100.0F ? 0.0F : Mth.clamp(((float)this.waterVisionTime - 100.0F) / 500.0F, 0.0F, 1.0F);
            return f2 * 0.6F + f3 * 0.39999998F;
         }
      }
   }

   public boolean isUnderWater() {
      return this.wasUnderwater;
   }

   protected boolean updateIsUnderwater() {
      boolean flag = this.wasUnderwater;
      boolean flag1 = super.updateIsUnderwater();
      if (this.isSpectator()) {
         return this.wasUnderwater;
      } else {
         if (!flag && flag1) {
            this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0F, 1.0F, false);
            this.minecraft.getSoundManager().play(new UnderwaterAmbientSoundInstances.UnderwaterAmbientSoundInstance(this));
         }

         if (flag && !flag1) {
            this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }

         return this.wasUnderwater;
      }
   }

   public Vec3 getRopeHoldPosition(float pPartialTick) {
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         float f = Mth.lerp(pPartialTick * 0.5F, this.getYRot(), this.yRotO) * ((float)Math.PI / 180F);
         float f1 = Mth.lerp(pPartialTick * 0.5F, this.getXRot(), this.xRotO) * ((float)Math.PI / 180F);
         double d0 = this.getMainArm() == HumanoidArm.RIGHT ? -1.0D : 1.0D;
         Vec3 vec3 = new Vec3(0.39D * d0, -0.6D, 0.3D);
         return vec3.xRot(-f1).yRot(-f).add(this.getEyePosition(pPartialTick));
      } else {
         return super.getRopeHoldPosition(pPartialTick);
      }
   }

   public void updateSyncFields(LocalPlayer old) {
      this.xLast = old.xLast;
      this.yLast1 = old.yLast1;
      this.zLast = old.zLast;
      this.yRotLast = old.yRotLast;
      this.xRotLast = old.xRotLast;
      this.lastOnGround = old.lastOnGround;
      this.wasShiftKeyDown = old.wasShiftKeyDown;
      this.wasSprinting = old.wasSprinting;
      this.positionReminder = old.positionReminder;
   }

   public void updateTutorialInventoryAction(ItemStack pCarried, ItemStack pClicked, ClickAction pAction) {
      this.minecraft.getTutorial().onInventoryAction(pCarried, pClicked, pAction);
   }

   public float getVisualRotationYInDegrees() {
      return this.getYRot();
   }
}
