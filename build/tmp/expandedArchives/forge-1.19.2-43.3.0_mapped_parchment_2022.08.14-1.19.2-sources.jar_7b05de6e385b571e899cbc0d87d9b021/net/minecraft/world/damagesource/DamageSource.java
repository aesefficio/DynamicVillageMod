package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

public class DamageSource {
   public static final DamageSource IN_FIRE = (new DamageSource("inFire")).bypassArmor().setIsFire();
   public static final DamageSource LIGHTNING_BOLT = new DamageSource("lightningBolt");
   public static final DamageSource ON_FIRE = (new DamageSource("onFire")).bypassArmor().setIsFire();
   public static final DamageSource LAVA = (new DamageSource("lava")).setIsFire();
   public static final DamageSource HOT_FLOOR = (new DamageSource("hotFloor")).setIsFire();
   public static final DamageSource IN_WALL = (new DamageSource("inWall")).bypassArmor();
   public static final DamageSource CRAMMING = (new DamageSource("cramming")).bypassArmor();
   public static final DamageSource DROWN = (new DamageSource("drown")).bypassArmor();
   public static final DamageSource STARVE = (new DamageSource("starve")).bypassArmor().bypassMagic();
   public static final DamageSource CACTUS = new DamageSource("cactus");
   public static final DamageSource FALL = (new DamageSource("fall")).bypassArmor().setIsFall();
   public static final DamageSource FLY_INTO_WALL = (new DamageSource("flyIntoWall")).bypassArmor();
   public static final DamageSource OUT_OF_WORLD = (new DamageSource("outOfWorld")).bypassArmor().bypassInvul();
   public static final DamageSource GENERIC = (new DamageSource("generic")).bypassArmor();
   public static final DamageSource MAGIC = (new DamageSource("magic")).bypassArmor().setMagic();
   public static final DamageSource WITHER = (new DamageSource("wither")).bypassArmor();
   public static final DamageSource ANVIL = (new DamageSource("anvil")).damageHelmet();
   public static final DamageSource FALLING_BLOCK = (new DamageSource("fallingBlock")).damageHelmet();
   public static final DamageSource DRAGON_BREATH = (new DamageSource("dragonBreath")).bypassArmor();
   public static final DamageSource DRY_OUT = new DamageSource("dryout");
   public static final DamageSource SWEET_BERRY_BUSH = new DamageSource("sweetBerryBush");
   public static final DamageSource FREEZE = (new DamageSource("freeze")).bypassArmor();
   public static final DamageSource FALLING_STALACTITE = (new DamageSource("fallingStalactite")).damageHelmet();
   public static final DamageSource STALAGMITE = (new DamageSource("stalagmite")).bypassArmor().setIsFall();
   private boolean damageHelmet;
   private boolean bypassArmor;
   private boolean bypassInvul;
   /** Whether or not the damage ignores modification by potion effects or enchantments. */
   private boolean bypassMagic;
   private boolean bypassEnchantments;
   private float exhaustion = 0.1F;
   private boolean isFireSource;
   private boolean isProjectile;
   private boolean scalesWithDifficulty;
   private boolean isMagic;
   private boolean isExplosion;
   private boolean isFall;
   private boolean noAggro;
   public final String msgId;

   public static DamageSource sting(LivingEntity pBee) {
      return new EntityDamageSource("sting", pBee);
   }

   public static DamageSource mobAttack(LivingEntity pMob) {
      return new EntityDamageSource("mob", pMob);
   }

   public static DamageSource indirectMobAttack(Entity pSource, @Nullable LivingEntity pIndirectEntity) {
      return new IndirectEntityDamageSource("mob", pSource, pIndirectEntity);
   }

   /**
    * returns an EntityDamageSource of type player
    */
   public static DamageSource playerAttack(Player pPlayer) {
      return new EntityDamageSource("player", pPlayer);
   }

   /**
    * returns EntityDamageSourceIndirect of an arrow
    */
   public static DamageSource arrow(AbstractArrow pArrow, @Nullable Entity pIndirectEntity) {
      return (new IndirectEntityDamageSource("arrow", pArrow, pIndirectEntity)).setProjectile();
   }

   public static DamageSource trident(Entity pSource, @Nullable Entity pIndirectEntity) {
      return (new IndirectEntityDamageSource("trident", pSource, pIndirectEntity)).setProjectile();
   }

   public static DamageSource fireworks(FireworkRocketEntity pFirework, @Nullable Entity pIndirectEntity) {
      return (new IndirectEntityDamageSource("fireworks", pFirework, pIndirectEntity)).setExplosion();
   }

   public static DamageSource fireball(Fireball pFireball, @Nullable Entity pIndirectEntity) {
      return pIndirectEntity == null ? (new IndirectEntityDamageSource("onFire", pFireball, pFireball)).setIsFire().setProjectile() : (new IndirectEntityDamageSource("fireball", pFireball, pIndirectEntity)).setIsFire().setProjectile();
   }

   public static DamageSource witherSkull(WitherSkull pWitherSkull, Entity pIndirectEntity) {
      return (new IndirectEntityDamageSource("witherSkull", pWitherSkull, pIndirectEntity)).setProjectile();
   }

   public static DamageSource thrown(Entity pSource, @Nullable Entity pIndirectEntity) {
      return (new IndirectEntityDamageSource("thrown", pSource, pIndirectEntity)).setProjectile();
   }

   public static DamageSource indirectMagic(Entity pSource, @Nullable Entity pIndirectEntity) {
      return (new IndirectEntityDamageSource("indirectMagic", pSource, pIndirectEntity)).bypassArmor().setMagic();
   }

   /**
    * Returns the EntityDamageSource of the Thorns enchantment
    */
   public static DamageSource thorns(Entity pSource) {
      return (new EntityDamageSource("thorns", pSource)).setThorns().setMagic();
   }

   public static DamageSource explosion(@Nullable Explosion pExplosion) {
      return explosion(pExplosion != null ? pExplosion.getSourceMob() : null);
   }

   public static DamageSource explosion(@Nullable LivingEntity pLivingEntity) {
      return pLivingEntity != null ? (new EntityDamageSource("explosion.player", pLivingEntity)).setScalesWithDifficulty().setExplosion() : (new DamageSource("explosion")).setScalesWithDifficulty().setExplosion();
   }

   public static DamageSource sonicBoom(Entity p_216877_) {
      return (new EntityDamageSource("sonic_boom", p_216877_)).bypassArmor().bypassEnchantments().setMagic();
   }

   public static DamageSource badRespawnPointExplosion() {
      return new BadRespawnPointDamage();
   }

   public String toString() {
      return "DamageSource (" + this.msgId + ")";
   }

   /**
    * Returns true if the damage is projectile based.
    */
   public boolean isProjectile() {
      return this.isProjectile;
   }

   /**
    * Define the damage type as projectile based.
    */
   public DamageSource setProjectile() {
      this.isProjectile = true;
      return this;
   }

   public boolean isExplosion() {
      return this.isExplosion;
   }

   public DamageSource setExplosion() {
      this.isExplosion = true;
      return this;
   }

   public boolean isBypassArmor() {
      return this.bypassArmor;
   }

   public boolean isDamageHelmet() {
      return this.damageHelmet;
   }

   /**
    * How much satiate(food) is consumed by this DamageSource
    */
   public float getFoodExhaustion() {
      return this.exhaustion;
   }

   public boolean isBypassInvul() {
      return this.bypassInvul;
   }

   /**
    * Whether or not the damage ignores modification by potion effects or enchantments.
    */
   public boolean isBypassMagic() {
      return this.bypassMagic;
   }

   public boolean isBypassEnchantments() {
      return this.bypassEnchantments;
   }

   public DamageSource(String pMessageId) {
      this.msgId = pMessageId;
   }

   /**
    * Retrieves the immediate causer of the damage, e.g. the arrow entity, not its shooter
    */
   @Nullable
   public Entity getDirectEntity() {
      return this.getEntity();
   }

   /**
    * Retrieves the true causer of the damage, e.g. the player who fired an arrow, the shulker who fired the bullet,
    * etc.
    */
   @Nullable
   public Entity getEntity() {
      return null;
   }

   public DamageSource bypassArmor() {
      this.bypassArmor = true;
      this.exhaustion = 0.0F;
      return this;
   }

   public DamageSource damageHelmet() {
      this.damageHelmet = true;
      return this;
   }

   public DamageSource bypassInvul() {
      this.bypassInvul = true;
      return this;
   }

   /**
    * Sets a value indicating whether the damage is absolute (ignores modification by potion effects or enchantments),
    * and also clears out hunger damage.
    */
   public DamageSource bypassMagic() {
      this.bypassMagic = true;
      this.exhaustion = 0.0F;
      return this;
   }

   public DamageSource bypassEnchantments() {
      this.bypassEnchantments = true;
      return this;
   }

   /**
    * Define the damage type as fire based.
    */
   public DamageSource setIsFire() {
      this.isFireSource = true;
      return this;
   }

   public DamageSource setNoAggro() {
      this.noAggro = true;
      return this;
   }

   /**
    * Gets the death message that is displayed when the player dies
    */
   public Component getLocalizedDeathMessage(LivingEntity pLivingEntity) {
      LivingEntity livingentity = pLivingEntity.getKillCredit();
      String s = "death.attack." + this.msgId;
      String s1 = s + ".player";
      return livingentity != null ? Component.translatable(s1, pLivingEntity.getDisplayName(), livingentity.getDisplayName()) : Component.translatable(s, pLivingEntity.getDisplayName());
   }

   /**
    * Returns true if the damage is fire based.
    */
   public boolean isFire() {
      return this.isFireSource;
   }

   public boolean isNoAggro() {
      return this.noAggro;
   }

   /**
    * Return the name of damage type.
    */
   public String getMsgId() {
      return this.msgId;
   }

   /**
    * Set whether this damage source will have its damage amount scaled based on the current difficulty.
    */
   public DamageSource setScalesWithDifficulty() {
      this.scalesWithDifficulty = true;
      return this;
   }

   /**
    * Return whether this damage source will have its damage amount scaled based on the current difficulty.
    */
   public boolean scalesWithDifficulty() {
      return this.scalesWithDifficulty;
   }

   /**
    * Returns true if the damage is magic based.
    */
   public boolean isMagic() {
      return this.isMagic;
   }

   /**
    * Define the damage type as magic based.
    */
   public DamageSource setMagic() {
      this.isMagic = true;
      return this;
   }

   public boolean isFall() {
      return this.isFall;
   }

   public DamageSource setIsFall() {
      this.isFall = true;
      return this;
   }

   public boolean isCreativePlayer() {
      Entity entity = this.getEntity();
      return entity instanceof Player && ((Player)entity).getAbilities().instabuild;
   }

   /**
    * Gets the location from which the damage originates.
    */
   @Nullable
   public Vec3 getSourcePosition() {
      return null;
   }
}