package net.minecraft.data.info;

import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;

public class CommandsReport implements DataProvider {
   private final DataGenerator generator;

   public CommandsReport(DataGenerator pGenerator) {
      this.generator = pGenerator;
   }

   public void run(CachedOutput pOutput) throws IOException {
      Path path = this.generator.getOutputFolder(DataGenerator.Target.REPORTS).resolve("commands.json");
      CommandDispatcher<CommandSourceStack> commanddispatcher = (new Commands(Commands.CommandSelection.ALL, new CommandBuildContext(RegistryAccess.BUILTIN.get()))).getDispatcher();
      DataProvider.saveStable(pOutput, ArgumentUtils.serializeNodeToJson(commanddispatcher, commanddispatcher.getRoot()), path);
   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "Command Syntax";
   }
}