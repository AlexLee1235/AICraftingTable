package com.watermelon0117.aicraft.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.watermelon0117.aicraft.items.MainItem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Main;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class DynParticle extends Particle {
    private final float uo;
    private final float vo;
    protected float quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
    private final ItemStack itemStack;
    public DynParticle(ClientLevel lvl, double x, double y, double z,
                       double vx, double vy, double vz, ItemStack itemStack) {
        super(lvl, x, y, z, 0,0,0);
        this.gravity = 1.0F;
        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
        this.xd *= (double)0.1F;
        this.yd *= (double)0.1F;
        this.zd *= (double)0.1F;
        this.xd += vx;
        this.yd += vy;
        this.zd += vz;
        this.itemStack=itemStack;
    }
    protected float getU0() {
        return (this.uo + 1.0F) / 4.0F;
    }

    protected float getU1() {
        return this.uo / 4.0F;
    }

    protected float getV0() {
        return this.vo / 4.0F;
    }

    protected float getV1() {
        return (this.vo + 1.0F) / 4.0F;
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    // draw a single camera-facing quad
    @Override
    public void render(VertexConsumer vc, Camera cam, float pt) {
        RenderSystem.setShaderTexture(0, MainItem.renderer.getTexture(itemStack));
        Vec3 vec3 = cam.getPosition();
        float f = (float)(Mth.lerp((double)pt, this.xo, this.x) - vec3.x());
        float f1 = (float)(Mth.lerp((double)pt, this.yo, this.y) - vec3.y());
        float f2 = (float)(Mth.lerp((double)pt, this.zo, this.z) - vec3.z());
        Quaternion quaternion;
        if (this.roll == 0.0F) {
            quaternion = cam.rotation();
        } else {
            quaternion = new Quaternion(cam.rotation());
            float f3 = Mth.lerp(pt, this.oRoll, this.roll);
            quaternion.mul(Vector3f.ZP.rotation(f3));
        }

        Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
        vector3f1.transform(quaternion);
        Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float f4 = this.quadSize;

        for(int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.transform(quaternion);
            vector3f.mul(f4);
            vector3f.add(f, f1, f2);
        }

        float f7 = this.getU0();
        float f8 = this.getU1();
        float f5 = this.getV0();
        float f6 = this.getV1();
        int j = this.getLightColor(pt);
        vc.vertex((double)avector3f[0].x(), (double)avector3f[0].y(), (double)avector3f[0].z()).uv(f8, f6).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        vc.vertex((double)avector3f[1].x(), (double)avector3f[1].y(), (double)avector3f[1].z()).uv(f8, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        vc.vertex((double)avector3f[2].x(), (double)avector3f[2].y(), (double)avector3f[2].z()).uv(f7, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        vc.vertex((double)avector3f[3].x(), (double)avector3f[3].y(), (double)avector3f[3].z()).uv(f7, f6).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<DynParticleOption> {
        public Particle createParticle(DynParticleOption p_105677_, ClientLevel p_105678_, double p_105679_, double p_105680_, double p_105681_, double p_105682_, double p_105683_, double p_105684_) {
            return new DynParticle(p_105678_, p_105679_, p_105680_, p_105681_, p_105682_, p_105683_, p_105684_, p_105677_.getItem());
        }
    }
}