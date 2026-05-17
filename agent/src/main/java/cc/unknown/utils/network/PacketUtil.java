/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.util.concurrent.GenericFutureListener
 *  net.minecraft.client.network.NetHandlerPlayClient
 *  net.minecraft.network.NetworkManager
 *  net.minecraft.network.NetworkManager$InboundHandlerTuplePacketListener
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.INetHandlerPlayClient
 *  net.minecraft.network.play.server.S00PacketKeepAlive
 *  net.minecraft.network.play.server.S01PacketJoinGame
 *  net.minecraft.network.play.server.S02PacketChat
 *  net.minecraft.network.play.server.S03PacketTimeUpdate
 *  net.minecraft.network.play.server.S04PacketEntityEquipment
 *  net.minecraft.network.play.server.S05PacketSpawnPosition
 *  net.minecraft.network.play.server.S06PacketUpdateHealth
 *  net.minecraft.network.play.server.S07PacketRespawn
 *  net.minecraft.network.play.server.S08PacketPlayerPosLook
 *  net.minecraft.network.play.server.S09PacketHeldItemChange
 *  net.minecraft.network.play.server.S0APacketUseBed
 *  net.minecraft.network.play.server.S0BPacketAnimation
 *  net.minecraft.network.play.server.S0CPacketSpawnPlayer
 *  net.minecraft.network.play.server.S0DPacketCollectItem
 *  net.minecraft.network.play.server.S0EPacketSpawnObject
 *  net.minecraft.network.play.server.S0FPacketSpawnMob
 *  net.minecraft.network.play.server.S10PacketSpawnPainting
 *  net.minecraft.network.play.server.S11PacketSpawnExperienceOrb
 *  net.minecraft.network.play.server.S12PacketEntityVelocity
 *  net.minecraft.network.play.server.S13PacketDestroyEntities
 *  net.minecraft.network.play.server.S14PacketEntity
 *  net.minecraft.network.play.server.S18PacketEntityTeleport
 *  net.minecraft.network.play.server.S19PacketEntityHeadLook
 *  net.minecraft.network.play.server.S19PacketEntityStatus
 *  net.minecraft.network.play.server.S1BPacketEntityAttach
 *  net.minecraft.network.play.server.S1CPacketEntityMetadata
 *  net.minecraft.network.play.server.S1DPacketEntityEffect
 *  net.minecraft.network.play.server.S1EPacketRemoveEntityEffect
 *  net.minecraft.network.play.server.S1FPacketSetExperience
 *  net.minecraft.network.play.server.S20PacketEntityProperties
 *  net.minecraft.network.play.server.S21PacketChunkData
 *  net.minecraft.network.play.server.S22PacketMultiBlockChange
 *  net.minecraft.network.play.server.S23PacketBlockChange
 *  net.minecraft.network.play.server.S24PacketBlockAction
 *  net.minecraft.network.play.server.S25PacketBlockBreakAnim
 *  net.minecraft.network.play.server.S26PacketMapChunkBulk
 *  net.minecraft.network.play.server.S27PacketExplosion
 *  net.minecraft.network.play.server.S28PacketEffect
 *  net.minecraft.network.play.server.S29PacketSoundEffect
 *  net.minecraft.network.play.server.S2APacketParticles
 *  net.minecraft.network.play.server.S2BPacketChangeGameState
 *  net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity
 *  net.minecraft.network.play.server.S2DPacketOpenWindow
 *  net.minecraft.network.play.server.S2EPacketCloseWindow
 *  net.minecraft.network.play.server.S2FPacketSetSlot
 *  net.minecraft.network.play.server.S30PacketWindowItems
 *  net.minecraft.network.play.server.S31PacketWindowProperty
 *  net.minecraft.network.play.server.S32PacketConfirmTransaction
 *  net.minecraft.network.play.server.S33PacketUpdateSign
 *  net.minecraft.network.play.server.S34PacketMaps
 *  net.minecraft.network.play.server.S35PacketUpdateTileEntity
 *  net.minecraft.network.play.server.S36PacketSignEditorOpen
 *  net.minecraft.network.play.server.S37PacketStatistics
 *  net.minecraft.network.play.server.S38PacketPlayerListItem
 *  net.minecraft.network.play.server.S39PacketPlayerAbilities
 *  net.minecraft.network.play.server.S3APacketTabComplete
 *  net.minecraft.network.play.server.S3BPacketScoreboardObjective
 *  net.minecraft.network.play.server.S3CPacketUpdateScore
 *  net.minecraft.network.play.server.S3DPacketDisplayScoreboard
 *  net.minecraft.network.play.server.S3EPacketTeams
 *  net.minecraft.network.play.server.S3FPacketCustomPayload
 *  net.minecraft.network.play.server.S40PacketDisconnect
 *  net.minecraft.network.play.server.S41PacketServerDifficulty
 *  net.minecraft.network.play.server.S42PacketCombatEvent
 *  net.minecraft.network.play.server.S43PacketCamera
 *  net.minecraft.network.play.server.S44PacketWorldBorder
 *  net.minecraft.network.play.server.S45PacketTitle
 *  net.minecraft.network.play.server.S46PacketSetCompressionLevel
 *  net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter
 *  net.minecraft.network.play.server.S48PacketResourcePackSend
 *  net.minecraft.network.play.server.S49PacketUpdateEntityNBT
 */
package cc.unknown.utils.network;

import cc.unknown.mixin.interfaces.network.INetHandlerPlayClient;
import cc.unknown.mixin.interfaces.network.INetworkManager;
import cc.unknown.utils.Loona;
import cc.unknown.utils.network.TimedPacket;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S05PacketSpawnPosition;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0APacketUseBed;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S10PacketSpawnPainting;
import net.minecraft.network.play.server.S11PacketSpawnExperienceOrb;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1EPacketRemoveEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S31PacketWindowProperty;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import net.minecraft.network.play.server.S34PacketMaps;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.network.play.server.S36PacketSignEditorOpen;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S3APacketTabComplete;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.network.play.server.S41PacketServerDifficulty;
import net.minecraft.network.play.server.S42PacketCombatEvent;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S44PacketWorldBorder;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.network.play.server.S46PacketSetCompressionLevel;
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import net.minecraft.network.play.server.S49PacketUpdateEntityNBT;

public class PacketUtil
implements Loona {
    public static final ConcurrentLinkedQueue<TimedPacket> packets = new ConcurrentLinkedQueue();

    public static void sendPacketNoEvent(Packet<?> i) {
        ((INetworkManager)mc.func_147114_u().func_147298_b()).sendPacketNoEvent(i);
    }

    public static void receivePacketNoEvent(Packet<?> i) {
        ((INetHandlerPlayClient)mc.func_147114_u()).receiveQueueNoEvent(i);
    }

    public static void send(Packet<?> i) {
        mc.func_147114_u().func_147297_a(i);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void send(Packet<?>[] i) {
        NetworkManager netManager;
        NetworkManager networkManager = netManager = mc.func_147114_u() != null ? mc.func_147114_u().func_147298_b() : null;
        if (netManager != null && netManager.func_150724_d()) {
            netManager.func_150733_h();
            for (Packet<?> packet : i) {
                netManager.func_150732_b(packet, null);
            }
        } else if (netManager != null) {
            try {
                netManager.field_181680_j.writeLock().lock();
                for (Packet<?> packet : i) {
                    netManager.field_150745_j.add(new NetworkManager.InboundHandlerTuplePacketListener(packet, Arrays.asList(new GenericFutureListener[]{null}).toArray(new GenericFutureListener[0])));
                }
            }
            finally {
                netManager.field_181680_j.writeLock().unlock();
            }
        }
    }

    public static void handlePacket(Packet<? extends net.minecraft.network.play.INetHandlerPlayClient> packet) {
        NetHandlerPlayClient netHandler = mc.func_147114_u();
        if (packet instanceof S00PacketKeepAlive) {
            netHandler.func_147272_a((S00PacketKeepAlive)packet);
        } else if (packet instanceof S01PacketJoinGame) {
            netHandler.func_147282_a((S01PacketJoinGame)packet);
        } else if (packet instanceof S02PacketChat) {
            netHandler.func_147251_a((S02PacketChat)packet);
        } else if (packet instanceof S03PacketTimeUpdate) {
            netHandler.func_147285_a((S03PacketTimeUpdate)packet);
        } else if (packet instanceof S04PacketEntityEquipment) {
            netHandler.func_147242_a((S04PacketEntityEquipment)packet);
        } else if (packet instanceof S05PacketSpawnPosition) {
            netHandler.func_147271_a((S05PacketSpawnPosition)packet);
        } else if (packet instanceof S06PacketUpdateHealth) {
            netHandler.func_147249_a((S06PacketUpdateHealth)packet);
        } else if (packet instanceof S07PacketRespawn) {
            netHandler.func_147280_a((S07PacketRespawn)packet);
        } else if (packet instanceof S08PacketPlayerPosLook) {
            netHandler.func_147258_a((S08PacketPlayerPosLook)packet);
        } else if (packet instanceof S09PacketHeldItemChange) {
            netHandler.func_147257_a((S09PacketHeldItemChange)packet);
        } else if (packet instanceof S10PacketSpawnPainting) {
            netHandler.func_147288_a((S10PacketSpawnPainting)packet);
        } else if (packet instanceof S0APacketUseBed) {
            netHandler.func_147278_a((S0APacketUseBed)packet);
        } else if (packet instanceof S0BPacketAnimation) {
            netHandler.func_147279_a((S0BPacketAnimation)packet);
        } else if (packet instanceof S0CPacketSpawnPlayer) {
            netHandler.func_147237_a((S0CPacketSpawnPlayer)packet);
        } else if (packet instanceof S0DPacketCollectItem) {
            netHandler.func_147246_a((S0DPacketCollectItem)packet);
        } else if (packet instanceof S0EPacketSpawnObject) {
            netHandler.func_147235_a((S0EPacketSpawnObject)packet);
        } else if (packet instanceof S0FPacketSpawnMob) {
            netHandler.func_147281_a((S0FPacketSpawnMob)packet);
        } else if (packet instanceof S11PacketSpawnExperienceOrb) {
            netHandler.func_147286_a((S11PacketSpawnExperienceOrb)packet);
        } else if (packet instanceof S12PacketEntityVelocity) {
            netHandler.func_147244_a((S12PacketEntityVelocity)packet);
        } else if (packet instanceof S13PacketDestroyEntities) {
            netHandler.func_147238_a((S13PacketDestroyEntities)packet);
        } else if (packet instanceof S14PacketEntity) {
            netHandler.func_147259_a((S14PacketEntity)packet);
        } else if (packet instanceof S18PacketEntityTeleport) {
            netHandler.func_147275_a((S18PacketEntityTeleport)packet);
        } else if (packet instanceof S19PacketEntityStatus) {
            netHandler.func_147236_a((S19PacketEntityStatus)packet);
        } else if (packet instanceof S19PacketEntityHeadLook) {
            netHandler.func_147267_a((S19PacketEntityHeadLook)packet);
        } else if (packet instanceof S1BPacketEntityAttach) {
            netHandler.func_147243_a((S1BPacketEntityAttach)packet);
        } else if (packet instanceof S1CPacketEntityMetadata) {
            netHandler.func_147284_a((S1CPacketEntityMetadata)packet);
        } else if (packet instanceof S1DPacketEntityEffect) {
            netHandler.func_147260_a((S1DPacketEntityEffect)packet);
        } else if (packet instanceof S1EPacketRemoveEntityEffect) {
            netHandler.func_147262_a((S1EPacketRemoveEntityEffect)packet);
        } else if (packet instanceof S1FPacketSetExperience) {
            netHandler.func_147295_a((S1FPacketSetExperience)packet);
        } else if (packet instanceof S20PacketEntityProperties) {
            netHandler.func_147290_a((S20PacketEntityProperties)packet);
        } else if (packet instanceof S21PacketChunkData) {
            netHandler.func_147263_a((S21PacketChunkData)packet);
        } else if (packet instanceof S22PacketMultiBlockChange) {
            netHandler.func_147287_a((S22PacketMultiBlockChange)packet);
        } else if (packet instanceof S23PacketBlockChange) {
            netHandler.func_147234_a((S23PacketBlockChange)packet);
        } else if (packet instanceof S24PacketBlockAction) {
            netHandler.func_147261_a((S24PacketBlockAction)packet);
        } else if (packet instanceof S25PacketBlockBreakAnim) {
            netHandler.func_147294_a((S25PacketBlockBreakAnim)packet);
        } else if (packet instanceof S26PacketMapChunkBulk) {
            netHandler.func_147269_a((S26PacketMapChunkBulk)packet);
        } else if (packet instanceof S27PacketExplosion) {
            netHandler.func_147283_a((S27PacketExplosion)packet);
        } else if (packet instanceof S28PacketEffect) {
            netHandler.func_147277_a((S28PacketEffect)packet);
        } else if (packet instanceof S29PacketSoundEffect) {
            netHandler.func_147255_a((S29PacketSoundEffect)packet);
        } else if (packet instanceof S2APacketParticles) {
            netHandler.func_147289_a((S2APacketParticles)packet);
        } else if (packet instanceof S2BPacketChangeGameState) {
            netHandler.func_147252_a((S2BPacketChangeGameState)packet);
        } else if (packet instanceof S2CPacketSpawnGlobalEntity) {
            netHandler.func_147292_a((S2CPacketSpawnGlobalEntity)packet);
        } else if (packet instanceof S2DPacketOpenWindow) {
            netHandler.func_147265_a((S2DPacketOpenWindow)packet);
        } else if (packet instanceof S2EPacketCloseWindow) {
            netHandler.func_147276_a((S2EPacketCloseWindow)packet);
        } else if (packet instanceof S2FPacketSetSlot) {
            netHandler.func_147266_a((S2FPacketSetSlot)packet);
        } else if (packet instanceof S30PacketWindowItems) {
            netHandler.func_147241_a((S30PacketWindowItems)packet);
        } else if (packet instanceof S31PacketWindowProperty) {
            netHandler.func_147245_a((S31PacketWindowProperty)packet);
        } else if (packet instanceof S32PacketConfirmTransaction) {
            netHandler.func_147239_a((S32PacketConfirmTransaction)packet);
        } else if (packet instanceof S33PacketUpdateSign) {
            netHandler.func_147248_a((S33PacketUpdateSign)packet);
        } else if (packet instanceof S34PacketMaps) {
            netHandler.func_147264_a((S34PacketMaps)packet);
        } else if (packet instanceof S35PacketUpdateTileEntity) {
            netHandler.func_147273_a((S35PacketUpdateTileEntity)packet);
        } else if (packet instanceof S36PacketSignEditorOpen) {
            netHandler.func_147268_a((S36PacketSignEditorOpen)packet);
        } else if (packet instanceof S37PacketStatistics) {
            netHandler.func_147293_a((S37PacketStatistics)packet);
        } else if (packet instanceof S38PacketPlayerListItem) {
            netHandler.func_147256_a((S38PacketPlayerListItem)packet);
        } else if (packet instanceof S39PacketPlayerAbilities) {
            netHandler.func_147270_a((S39PacketPlayerAbilities)packet);
        } else if (packet instanceof S3APacketTabComplete) {
            netHandler.func_147274_a((S3APacketTabComplete)packet);
        } else if (packet instanceof S3BPacketScoreboardObjective) {
            netHandler.func_147291_a((S3BPacketScoreboardObjective)packet);
        } else if (packet instanceof S3CPacketUpdateScore) {
            netHandler.func_147250_a((S3CPacketUpdateScore)packet);
        } else if (packet instanceof S3DPacketDisplayScoreboard) {
            netHandler.func_147254_a((S3DPacketDisplayScoreboard)packet);
        } else if (packet instanceof S3EPacketTeams) {
            netHandler.func_147247_a((S3EPacketTeams)packet);
        } else if (packet instanceof S3FPacketCustomPayload) {
            netHandler.func_147240_a((S3FPacketCustomPayload)packet);
        } else if (packet instanceof S40PacketDisconnect) {
            netHandler.func_147253_a((S40PacketDisconnect)packet);
        } else if (packet instanceof S41PacketServerDifficulty) {
            netHandler.func_175101_a((S41PacketServerDifficulty)packet);
        } else if (packet instanceof S42PacketCombatEvent) {
            netHandler.func_175098_a((S42PacketCombatEvent)packet);
        } else if (packet instanceof S43PacketCamera) {
            netHandler.func_175094_a((S43PacketCamera)packet);
        } else if (packet instanceof S44PacketWorldBorder) {
            netHandler.func_175093_a((S44PacketWorldBorder)packet);
        } else if (packet instanceof S45PacketTitle) {
            netHandler.func_175099_a((S45PacketTitle)packet);
        } else if (packet instanceof S46PacketSetCompressionLevel) {
            netHandler.func_175100_a((S46PacketSetCompressionLevel)packet);
        } else if (packet instanceof S47PacketPlayerListHeaderFooter) {
            netHandler.func_175096_a((S47PacketPlayerListHeaderFooter)packet);
        } else if (packet instanceof S48PacketResourcePackSend) {
            netHandler.func_175095_a((S48PacketResourcePackSend)packet);
        } else if (packet instanceof S49PacketUpdateEntityNBT) {
            netHandler.func_175097_a((S49PacketUpdateEntityNBT)packet);
        }
    }
}

