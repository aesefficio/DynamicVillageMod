package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class SkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
   private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   private final LongSet sectionsWithSources = new LongOpenHashSet();
   private final LongSet sectionsToAddSourcesTo = new LongOpenHashSet();
   private final LongSet sectionsToRemoveSourcesFrom = new LongOpenHashSet();
   private final LongSet columnsWithSkySources = new LongOpenHashSet();
   private volatile boolean hasSourceInconsistencies;

   protected SkyLightSectionStorage(LightChunkGetter pChunkSource) {
      super(LightLayer.SKY, pChunkSource, new SkyLightSectionStorage.SkyDataLayerStorageMap(new Long2ObjectOpenHashMap<>(), new Long2IntOpenHashMap(), Integer.MAX_VALUE));
   }

   protected int getLightValue(long pLevelPos) {
      return this.getLightValue(pLevelPos, false);
   }

   protected int getLightValue(long pPackedPos, boolean pUpdateAll) {
      long i = SectionPos.blockToSection(pPackedPos);
      int j = SectionPos.y(i);
      SkyLightSectionStorage.SkyDataLayerStorageMap skylightsectionstorage$skydatalayerstoragemap = pUpdateAll ? this.updatingSectionData : this.visibleSectionData;
      int k = skylightsectionstorage$skydatalayerstoragemap.topSections.get(SectionPos.getZeroNode(i));
      if (k != skylightsectionstorage$skydatalayerstoragemap.currentLowestY && j < k) {
         DataLayer datalayer = this.getDataLayer(skylightsectionstorage$skydatalayerstoragemap, i);
         if (datalayer == null) {
            for(pPackedPos = BlockPos.getFlatIndex(pPackedPos); datalayer == null; datalayer = this.getDataLayer(skylightsectionstorage$skydatalayerstoragemap, i)) {
               ++j;
               if (j >= k) {
                  return 15;
               }

               pPackedPos = BlockPos.offset(pPackedPos, 0, 16, 0);
               i = SectionPos.offset(i, Direction.UP);
            }
         }

         return datalayer.get(SectionPos.sectionRelative(BlockPos.getX(pPackedPos)), SectionPos.sectionRelative(BlockPos.getY(pPackedPos)), SectionPos.sectionRelative(BlockPos.getZ(pPackedPos)));
      } else {
         return pUpdateAll && !this.lightOnInSection(i) ? 0 : 15;
      }
   }

   protected void onNodeAdded(long pSectionPos) {
      int i = SectionPos.y(pSectionPos);
      if ((this.updatingSectionData).currentLowestY > i) {
         (this.updatingSectionData).currentLowestY = i;
         (this.updatingSectionData).topSections.defaultReturnValue((this.updatingSectionData).currentLowestY);
      }

      long j = SectionPos.getZeroNode(pSectionPos);
      int k = (this.updatingSectionData).topSections.get(j);
      if (k < i + 1) {
         (this.updatingSectionData).topSections.put(j, i + 1);
         if (this.columnsWithSkySources.contains(j)) {
            this.queueAddSource(pSectionPos);
            if (k > (this.updatingSectionData).currentLowestY) {
               long l = SectionPos.asLong(SectionPos.x(pSectionPos), k - 1, SectionPos.z(pSectionPos));
               this.queueRemoveSource(l);
            }

            this.recheckInconsistencyFlag();
         }
      }

   }

   private void queueRemoveSource(long p_75895_) {
      this.sectionsToRemoveSourcesFrom.add(p_75895_);
      this.sectionsToAddSourcesTo.remove(p_75895_);
   }

   private void queueAddSource(long p_75897_) {
      this.sectionsToAddSourcesTo.add(p_75897_);
      this.sectionsToRemoveSourcesFrom.remove(p_75897_);
   }

   private void recheckInconsistencyFlag() {
      this.hasSourceInconsistencies = !this.sectionsToAddSourcesTo.isEmpty() || !this.sectionsToRemoveSourcesFrom.isEmpty();
   }

   protected void onNodeRemoved(long pSectionPos) {
      long i = SectionPos.getZeroNode(pSectionPos);
      boolean flag = this.columnsWithSkySources.contains(i);
      if (flag) {
         this.queueRemoveSource(pSectionPos);
      }

      int j = SectionPos.y(pSectionPos);
      if ((this.updatingSectionData).topSections.get(i) == j + 1) {
         long k;
         for(k = pSectionPos; !this.storingLightForSection(k) && this.hasSectionsBelow(j); k = SectionPos.offset(k, Direction.DOWN)) {
            --j;
         }

         if (this.storingLightForSection(k)) {
            (this.updatingSectionData).topSections.put(i, j + 1);
            if (flag) {
               this.queueAddSource(k);
            }
         } else {
            (this.updatingSectionData).topSections.remove(i);
         }
      }

      if (flag) {
         this.recheckInconsistencyFlag();
      }

   }

   protected void enableLightSources(long pSectionPos, boolean pIsQueueEmpty) {
      this.runAllUpdates();
      if (pIsQueueEmpty && this.columnsWithSkySources.add(pSectionPos)) {
         int i = (this.updatingSectionData).topSections.get(pSectionPos);
         if (i != (this.updatingSectionData).currentLowestY) {
            long j = SectionPos.asLong(SectionPos.x(pSectionPos), i - 1, SectionPos.z(pSectionPos));
            this.queueAddSource(j);
            this.recheckInconsistencyFlag();
         }
      } else if (!pIsQueueEmpty) {
         this.columnsWithSkySources.remove(pSectionPos);
      }

   }

   protected boolean hasInconsistencies() {
      return super.hasInconsistencies() || this.hasSourceInconsistencies;
   }

   protected DataLayer createDataLayer(long pSectionPos) {
      DataLayer datalayer = this.queuedSections.get(pSectionPos);
      if (datalayer != null) {
         return datalayer;
      } else {
         long i = SectionPos.offset(pSectionPos, Direction.UP);
         int j = (this.updatingSectionData).topSections.get(SectionPos.getZeroNode(pSectionPos));
         if (j != (this.updatingSectionData).currentLowestY && SectionPos.y(i) < j) {
            DataLayer datalayer1;
            while((datalayer1 = this.getDataLayer(i, true)) == null) {
               i = SectionPos.offset(i, Direction.UP);
            }

            return repeatFirstLayer(datalayer1);
         } else {
            return new DataLayer();
         }
      }
   }

   private static DataLayer repeatFirstLayer(DataLayer p_182513_) {
      if (p_182513_.isEmpty()) {
         return new DataLayer();
      } else {
         byte[] abyte = p_182513_.getData();
         byte[] abyte1 = new byte[2048];

         for(int i = 0; i < 16; ++i) {
            System.arraycopy(abyte, 0, abyte1, i * 128, 128);
         }

         return new DataLayer(abyte1);
      }
   }

   protected void markNewInconsistencies(LayerLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, ?> pEngine, boolean pUpdateSkyLight, boolean pUpdateBlockLight) {
      super.markNewInconsistencies(pEngine, pUpdateSkyLight, pUpdateBlockLight);
      if (pUpdateSkyLight) {
         if (!this.sectionsToAddSourcesTo.isEmpty()) {
            for(long i : this.sectionsToAddSourcesTo) {
               int j = this.getLevel(i);
               if (j != 2 && !this.sectionsToRemoveSourcesFrom.contains(i) && this.sectionsWithSources.add(i)) {
                  if (j == 1) {
                     this.clearQueuedSectionBlocks(pEngine, i);
                     if (this.changedSections.add(i)) {
                        this.updatingSectionData.copyDataLayer(i);
                     }

                     Arrays.fill(this.getDataLayer(i, true).getData(), (byte)-1);
                     int i3 = SectionPos.sectionToBlockCoord(SectionPos.x(i));
                     int k3 = SectionPos.sectionToBlockCoord(SectionPos.y(i));
                     int i4 = SectionPos.sectionToBlockCoord(SectionPos.z(i));

                     for(Direction direction : HORIZONTALS) {
                        long j1 = SectionPos.offset(i, direction);
                        if ((this.sectionsToRemoveSourcesFrom.contains(j1) || !this.sectionsWithSources.contains(j1) && !this.sectionsToAddSourcesTo.contains(j1)) && this.storingLightForSection(j1)) {
                           for(int k1 = 0; k1 < 16; ++k1) {
                              for(int l1 = 0; l1 < 16; ++l1) {
                                 long i2;
                                 long j2;
                                 switch (direction) {
                                    case NORTH:
                                       i2 = BlockPos.asLong(i3 + k1, k3 + l1, i4);
                                       j2 = BlockPos.asLong(i3 + k1, k3 + l1, i4 - 1);
                                       break;
                                    case SOUTH:
                                       i2 = BlockPos.asLong(i3 + k1, k3 + l1, i4 + 16 - 1);
                                       j2 = BlockPos.asLong(i3 + k1, k3 + l1, i4 + 16);
                                       break;
                                    case WEST:
                                       i2 = BlockPos.asLong(i3, k3 + k1, i4 + l1);
                                       j2 = BlockPos.asLong(i3 - 1, k3 + k1, i4 + l1);
                                       break;
                                    default:
                                       i2 = BlockPos.asLong(i3 + 16 - 1, k3 + k1, i4 + l1);
                                       j2 = BlockPos.asLong(i3 + 16, k3 + k1, i4 + l1);
                                 }

                                 pEngine.checkEdge(i2, j2, pEngine.computeLevelFromNeighbor(i2, j2, 0), true);
                              }
                           }
                        }
                     }

                     for(int j4 = 0; j4 < 16; ++j4) {
                        for(int k4 = 0; k4 < 16; ++k4) {
                           long l4 = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(i), j4), SectionPos.sectionToBlockCoord(SectionPos.y(i)), SectionPos.sectionToBlockCoord(SectionPos.z(i), k4));
                           long i5 = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(i), j4), SectionPos.sectionToBlockCoord(SectionPos.y(i)) - 1, SectionPos.sectionToBlockCoord(SectionPos.z(i), k4));
                           pEngine.checkEdge(l4, i5, pEngine.computeLevelFromNeighbor(l4, i5, 0), true);
                        }
                     }
                  } else {
                     for(int k = 0; k < 16; ++k) {
                        for(int l = 0; l < 16; ++l) {
                           long i1 = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(i), k), SectionPos.sectionToBlockCoord(SectionPos.y(i), 15), SectionPos.sectionToBlockCoord(SectionPos.z(i), l));
                           pEngine.checkEdge(Long.MAX_VALUE, i1, 0, true);
                        }
                     }
                  }
               }
            }
         }

         this.sectionsToAddSourcesTo.clear();
         if (!this.sectionsToRemoveSourcesFrom.isEmpty()) {
            for(long k2 : this.sectionsToRemoveSourcesFrom) {
               if (this.sectionsWithSources.remove(k2) && this.storingLightForSection(k2)) {
                  for(int l2 = 0; l2 < 16; ++l2) {
                     for(int j3 = 0; j3 < 16; ++j3) {
                        long l3 = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(k2), l2), SectionPos.sectionToBlockCoord(SectionPos.y(k2), 15), SectionPos.sectionToBlockCoord(SectionPos.z(k2), j3));
                        pEngine.checkEdge(Long.MAX_VALUE, l3, 15, false);
                     }
                  }
               }
            }
         }

         this.sectionsToRemoveSourcesFrom.clear();
         this.hasSourceInconsistencies = false;
      }
   }

   protected boolean hasSectionsBelow(int pY) {
      return pY >= (this.updatingSectionData).currentLowestY;
   }

   protected boolean isAboveData(long pSectionPos) {
      long i = SectionPos.getZeroNode(pSectionPos);
      int j = (this.updatingSectionData).topSections.get(i);
      return j == (this.updatingSectionData).currentLowestY || SectionPos.y(pSectionPos) >= j;
   }

   protected boolean lightOnInSection(long pSectionPos) {
      long i = SectionPos.getZeroNode(pSectionPos);
      return this.columnsWithSkySources.contains(i);
   }

   protected static final class SkyDataLayerStorageMap extends DataLayerStorageMap<SkyLightSectionStorage.SkyDataLayerStorageMap> {
      int currentLowestY;
      final Long2IntOpenHashMap topSections;

      public SkyDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> pMap, Long2IntOpenHashMap pTopSections, int pCurrentLowestY) {
         super(pMap);
         this.topSections = pTopSections;
         pTopSections.defaultReturnValue(pCurrentLowestY);
         this.currentLowestY = pCurrentLowestY;
      }

      public SkyLightSectionStorage.SkyDataLayerStorageMap copy() {
         return new SkyLightSectionStorage.SkyDataLayerStorageMap(this.map.clone(), this.topSections.clone(), this.currentLowestY);
      }
   }
}