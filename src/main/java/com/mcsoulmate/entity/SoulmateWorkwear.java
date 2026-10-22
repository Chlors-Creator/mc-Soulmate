package com.mcsoulmate.entity;

public final class SoulmateWorkwear {
    private static final long TOGGLE_COOLDOWN_TICKS = 20L;

    private boolean wearingHat;
    private long nextToggleGameTime;

    public boolean isWearingHat() {
        return this.wearingHat;
    }

    public void update(SoulmateEntity soulmate, boolean wantsHat) {
        if (soulmate.level().isClientSide() || wantsHat == this.wearingHat) {
            return;
        }

        long gameTime = soulmate.level().getGameTime();
        if (gameTime < this.nextToggleGameTime) {
            return;
        }

        this.wearingHat = wantsHat;
        this.nextToggleGameTime = gameTime + TOGGLE_COOLDOWN_TICKS;
        soulmate.setWearingWorkHat(wantsHat);
    }
}
