package thut.essentials.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;

public class Coordinate implements Comparable<Coordinate>
{

    public static Coordinate getChunkCoordFromWorldCoord(final BlockPos pos, final int dimension)
    {
        return Coordinate.getChunkCoordFromWorldCoord(pos.getX(), pos.getY(), pos.getZ(), dimension);
    }

    public static Coordinate getChunkCoordFromWorldCoord(final BlockPos pos, final Dimension dimension)
    {
        return Coordinate.getChunkCoordFromWorldCoord(pos.getX(), pos.getY(), pos.getZ(), dimension.getType().getId());
    }

    public static Coordinate getChunkCoordFromWorldCoord(final int x2, final int y2, final int z2,
            final DimensionType dimension)
    {
        return Coordinate.getChunkCoordFromWorldCoord(x2, y2, z2, dimension.getId());
    }

    public static Coordinate getChunkCoordFromWorldCoord(final int x, final int y, final int z, final int dim)
    {
        final int i = MathHelper.floor(x / 16.0D);
        final int j = MathHelper.floor(y / 16.0D);
        final int k = MathHelper.floor(z / 16.0D);
        return new Coordinate(i, j, k, dim);
    }

    public int x;
    public int y;
    public int z;
    public int dim;

    public Coordinate(final BlockPos pos, final int dimension)
    {
        this(pos.getX(), pos.getY(), pos.getZ(), dimension);
    }

    public Coordinate(final BlockPos pos, final Dimension dimension)
    {
        this(pos.getX(), pos.getY(), pos.getZ(), dimension.getType().getId());
    }

    public Coordinate(final BlockPos pos, final DimensionType dimension)
    {
        this(pos.getX(), pos.getY(), pos.getZ(), dimension.getId());
    }

    public Coordinate(final int x, final int y, final int z, final int dim)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (!(obj instanceof Coordinate)) return false;
        final Coordinate BlockPos = (Coordinate) obj;
        return this.x == BlockPos.x && this.y == BlockPos.y && this.z == BlockPos.z && this.dim == BlockPos.dim;
    }

    @Override
    public int hashCode()
    {
        return this.x + this.z << 8 + this.y << 16 + this.dim << 24;
    }

    @Override
    public int compareTo(final Coordinate p_compareTo_1_)
    {
        return this.y == p_compareTo_1_.y ? this.z == p_compareTo_1_.z ? this.x - p_compareTo_1_.x
                : this.dim == p_compareTo_1_.dim ? this.z - p_compareTo_1_.z : this.dim - p_compareTo_1_.dim
                : this.y - p_compareTo_1_.y;
    }

    @Override
    public String toString()
    {
        return "CCxyzw: " + this.x + " " + this.y + " " + this.z + " " + this.dim;
    }
}
