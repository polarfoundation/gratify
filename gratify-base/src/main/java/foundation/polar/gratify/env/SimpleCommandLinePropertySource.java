package foundation.polar.gratify.env;

import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * {@link CommandLinePropertySource} implementation backed by a simple String array.
 *
 * <h3>Purpose</h3>
 * <p>This {@code CommandLinePropertySource} implementation aims to provide the simplest
 * possible approach to parsing command line arguments. As with all {@code
 * CommandLinePropertySource} implementations, command line arguments are broken into two
 * distinct groups: <em>option arguments</em> and <em>non-option arguments</em>, as
 * described below <em>(some sections copied from Javadoc for
 * {@link SimpleCommandLineArgsParser})</em>:
 *
 * <h3>Working with option arguments</h3>
 * <p>Option arguments must adhere to the exact syntax:
 *
 * <pre class="code">--optName[=optValue]</pre>
 *
 * <p>That is, options must be prefixed with "{@code --}" and may or may not
 * specify a value. If a value is specified, the name and value must be separated
 * <em>without spaces</em> by an equals sign ("="). The value may optionally be
 * an empty string.
 *
 * <h4>Valid examples of option arguments</h4>
 * <pre class="code">
 * --foo
 * --foo=
 * --foo=""
 * --foo=bar
 * --foo="bar then baz"
 * --foo=bar,baz,biz</pre>
 *
 * <h4>Invalid examples of option arguments</h4>
 * <pre class="code">
 * -foo
 * --foo bar
 * --foo = bar
 * --foo=bar --foo=baz --foo=biz</pre>
 *
 * <h3>Working with non-option arguments</h3>
 * <p>Any and all arguments specified at the command line without the "{@code --}"
 * option prefix will be considered as "non-option arguments" and made available
 * through the {@link CommandLineArgs#getNonOptionArgs()} method.
 *
 * <h3>Typical usage</h3>
 * <pre class="code">
 * public static void main(String[] args) {
 *     PropertySource<?> ps = new SimpleCommandLinePropertySource(args);
 *     // ...
 * }</pre>
 *
 * See {@link CommandLinePropertySource} for complete general usage examples.
 *
 * <h3>Beyond the basics</h3>
 *
 * <p>When more fully-featured command line parsing is necessary, consider using
 * the provided {@link JOptCommandLinePropertySource}, or implement your own
 * {@code CommandLinePropertySource} against the command line parsing library of your
 * choice.
 *
 * @author Chris Beams
 * @see CommandLinePropertySource
 * @see JOptCommandLinePropertySource
 */
public class SimpleCommandLinePropertySource extends CommandLinePropertySource<CommandLineArgs> {

   /**
    * Create a new {@code SimpleCommandLinePropertySource} having the default name
    * and backed by the given {@code String[]} of command line arguments.
    * @see CommandLinePropertySource#COMMAND_LINE_PROPERTY_SOURCE_NAME
    * @see CommandLinePropertySource#CommandLinePropertySource(Object)
    */
   public SimpleCommandLinePropertySource(String... args) {
      super(new SimpleCommandLineArgsParser().parse(args));
   }

   /**
    * Create a new {@code SimpleCommandLinePropertySource} having the given name
    * and backed by the given {@code String[]} of command line arguments.
    */
   public SimpleCommandLinePropertySource(String name, String[] args) {
      super(name, new SimpleCommandLineArgsParser().parse(args));
   }

   /**
    * Get the property names for the option arguments.
    */
   @Override
   public String[] getPropertyNames() {
      return StringUtils.toStringArray(this.source.getOptionNames());
   }

   @Override
   protected boolean containsOption(String name) {
      return this.source.containsOption(name);
   }

   @Override
   @Nullable
   protected List<String> getOptionValues(String name) {
      return this.source.getOptionValues(name);
   }

   @Override
   protected List<String> getNonOptionArgs() {
      return this.source.getNonOptionArgs();
   }

}
